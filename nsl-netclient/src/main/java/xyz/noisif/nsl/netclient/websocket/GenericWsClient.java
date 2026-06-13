/*
 * Copyright (c) 2022-2026 NOISIF. All Rights Reserved.
 *
 * NOTICE: This source code is publicly available for reference
 * and educational purposes only. It is NOT open-source software.
 *
 * You are granted permission to view this code. However, you are strictly
 * PROHIBITED from copying, modifying, or merging this code into other software,
 * distributing, publishing, or sublicensing this code, using this code for
 * commercial purposes or in production environments.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO WARRANTIES OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * Please refer to the LICENSE file in the root directory for full restrictions.
 */
package xyz.noisif.nsl.netclient.websocket;

import xyz.noisif.nsl.codec.envelope.OpCode;
import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.common.util.concurrent.ConcurrentUtil;
import xyz.noisif.nsl.common.util.concurrent.VirtualPeriodicTaskScheduler;
import xyz.noisif.nsl.common.util.io.IoUtil;
import xyz.noisif.nsl.common.util.thread.TaskExecutor;
import xyz.noisif.nsl.net.http.header.CommonHttpHeaderName;
import xyz.noisif.nsl.net.http.header.HttpHeaderName;
import xyz.noisif.nsl.net.lifecycle.NetworkSessionLifecycleListener;
import xyz.noisif.nsl.netclient.NetworkClient;
import xyz.noisif.nsl.netclient.group.ClientGroup;
import xyz.noisif.nsl.netclient.websocket.auth.WsClientAuthenticator;
import xyz.noisif.nsl.netclient.websocket.group.WsClientGroupConfig;
import xyz.noisif.nsl.netclient.websocket.group.codec.WsSessionCodec;
import xyz.noisif.nsl.netclient.websocket.heartbeat.WsHeartbeatManager;
import xyz.noisif.nsl.netclient.websocket.registry.InMemoryWsClientSessionRegistry;
import xyz.noisif.nsl.netclient.websocket.registry.WsClientSessionRegistry;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public abstract class GenericWsClient extends NetworkClient<WsClientGroupConfig>
    implements WsClient {
  protected final WsClientSessionRegistry sessionRegistry;

  private final ScheduledExecutorService ticker;
  private final TaskExecutor workerPool;
  private final WsHeartbeatManager heartbeatManager;
  private final WsReconnectManager reconnectManager;

  protected GenericWsClient(AbstractBuilder<?> builder) {
    super(builder);
    sessionRegistry = builder.sessionRegistry;
    ticker = ConcurrentUtil.singleThread("ws-ticker");
    workerPool = TaskExecutor.createDefault("ws-worker-pool");
    heartbeatManager =
        new WsHeartbeatManager(
            VirtualPeriodicTaskScheduler.create(ticker, workerPool.getDelegate()));
    reconnectManager = new WsReconnectManager(ticker);
  }

  @Override
  protected void onStart() throws Exception {
    final Set<Map.Entry<ClientGroup, WsClientGroupConfig>> groups =
        clientsRegistry.getEntries().entrySet();
    log.info("Starting WS client, registering {} connection groups", groups.size());
    for (final Map.Entry<ClientGroup, WsClientGroupConfig> clientGroup : groups) {
      connectWithRetry(clientGroup.getKey(), clientGroup.getValue(), 1);
    }
  }

  @Override
  protected void onStop() {
    heartbeatManager.stopAll();
    IoUtil.closeQuietly(ticker, ExecutorService::shutdownNow);
    IoUtil.closeQuietly(workerPool);
  }

  @Override
  public void send(ClientGroup clientGroup, byte[] message) {
    executeOnSession(clientGroup, "binary", session -> session.send(message));
  }

  @Override
  public void send(ClientGroup clientGroup, String message) {
    executeOnSession(clientGroup, "text", session -> session.send(message));
  }

  @Override
  public void sendObject(ClientGroup clientGroup, Object object) {
    executeOnSession(clientGroup, "object", session -> session.sendObject(object));
  }

  @Override
  public void sendEnvelope(ClientGroup clientGroup, OpCode opCode, Object data) {
    executeOnSession(clientGroup, "envelope", session -> session.sendEnvelope(opCode, data));
  }

  @Override
  public boolean isConnected(ClientGroup group) {
    final Collection<WsClientSession> sessions = sessionRegistry.getSessions(group);
    return sessions.stream().anyMatch(session -> !session.isClosed());
  }

  private void executeOnSession(
      ClientGroup clientGroup, String type, Consumer<WsClientSession> action) {
    if (log.isTraceEnabled()) {
      log.trace("Dispatching {} message to group {}", type, clientGroup.getClientGroupName());
    }
    sessionRegistry
        .getAnySession(clientGroup)
        .ifPresentOrElse(action, () -> logMissingSession(clientGroup, type));
  }

  private void logMissingSession(ClientGroup group, String type) {
    log.error(
        "Not found any session for WS group: {} (type: {})", group.getClientGroupName(), type);
  }

  private void connectWithRetry(ClientGroup group, WsClientGroupConfig config, int attempt) {
    final String groupName = group.getClientGroupName();
    log.debug("Initiating WS connection for group '{}' (attempt: {})", groupName, attempt);
    try {
      final var req = new WsClientUpgradeRequest(URI.create(config.getUrl()));
      applyAuthenticator(req, config);
      applyHeaders(req, config);
      final var lifecycleWrapper =
          new ManagedSessionLifecycleListener(
              group,
              config,
              heartbeatManager,
              reconnectManager,
              () -> connectWithRetry(group, config, attempt + 1));
      onClientGroupStart(
          group, config, req, config.getBusConfig().configureProtocol(req), lifecycleWrapper);
    } catch (Exception ex) {
      log.error("Failed to connect WS session for group '{}': {}", groupName, ex.getMessage());
      reconnectManager.handleFailure(
          group, config, attempt, () -> connectWithRetry(group, config, attempt + 1));
    }
  }

  private void applyAuthenticator(WsClientUpgradeRequest req, WsClientGroupConfig config) {
    final WsClientAuthenticator authenticator = config.getAuthenticator();
    if (authenticator != null) {
      log.debug(
          "Applying authenticator: {} for group: {}",
          authenticator.getClass().getSimpleName(),
          config.getPrincipalId());
      authenticator.applyAuthentication(req);
      return;
    }
    log.trace("No authenticator configured for group: {}", config.getPrincipalId());
  }

  private void applyHeaders(WsClientUpgradeRequest req, WsClientGroupConfig config) {
    log.trace("Applying custom headers for principal: {}", config.getPrincipalId());
    for (final Map.Entry<HttpHeaderName, String> h : config.getCustomHeaders().entrySet()) {
      if (log.isTraceEnabled()) {
        log.trace("Setting header: {} = {}", h.getKey().getCode(), h.getValue());
      }
      req.setHeader(h.getKey(), h.getValue());
    }
    log.trace("Setting User-Agent: {}", config.getPrincipalId());
    req.setHeader(CommonHttpHeaderName.USER_AGENT, config.getPrincipalId());
  }

  protected abstract void onClientGroupStart(
      ClientGroup clientGroup,
      WsClientGroupConfig config,
      WsClientUpgradeRequest req,
      WsSessionCodec sessionCodec,
      NetworkSessionLifecycleListener<WsClientSession> lifecycleListener)
      throws Exception;

  protected abstract static class AbstractBuilder<B extends AbstractBuilder<B>>
      extends AbstractBaseBuilder<WsClientGroupConfig, B> {
    private WsClientSessionRegistry sessionRegistry =
        InMemoryWsClientSessionRegistry.createDefault();

    protected AbstractBuilder() {
      super();
    }

    public B sessionRegistry(WsClientSessionRegistry sessionRegistry) {
      this.sessionRegistry = sessionRegistry;
      return self();
    }

    @Override
    protected void validate() {
      super.validate();
      Assert.notNull(sessionRegistry, "WsSessionRegistry cannot be null");
    }

    public abstract GenericWsClient build();
  }
}
