/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jwizard.jwl.netclient.websocket;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.common.util.concurrent.ConcurrentUtil;
import xyz.jwizard.jwl.common.util.concurrent.VirtualPeriodicTaskScheduler;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.common.util.thread.TaskExecutor;
import xyz.jwizard.jwl.net.http.header.CommonHttpHeaderName;
import xyz.jwizard.jwl.net.http.header.HttpHeaderName;
import xyz.jwizard.jwl.net.lifecycle.NetworkSessionLifecycleListener;
import xyz.jwizard.jwl.netclient.NetworkClient;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.websocket.auth.WsClientAuthenticator;
import xyz.jwizard.jwl.netclient.websocket.group.WsClientGroupConfig;
import xyz.jwizard.jwl.netclient.websocket.group.codec.WsSessionCodec;
import xyz.jwizard.jwl.netclient.websocket.heartbeat.WsHeartbeatManager;
import xyz.jwizard.jwl.netclient.websocket.registry.InMemoryWsClientSessionRegistry;
import xyz.jwizard.jwl.netclient.websocket.registry.WsClientSessionRegistry;

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
        heartbeatManager = new WsHeartbeatManager(VirtualPeriodicTaskScheduler
            .create(ticker, workerPool.getDelegate())
        );
        reconnectManager = new WsReconnectManager(ticker);
    }

    @Override
    protected void onStart() throws Exception {
        final Set<Map.Entry<ClientGroup, WsClientGroupConfig>> groups = clientsRegistry
            .getEntries().entrySet();
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

    private void executeOnSession(ClientGroup clientGroup, String type,
                                  Consumer<WsClientSession> action) {
        if (log.isTraceEnabled()) {
            log.trace("Dispatching {} message to group {}", type, clientGroup.getClientGroupName());
        }
        sessionRegistry.getAnySession(clientGroup).ifPresentOrElse(action,
            () -> log.error("Not found any session for WS group: {} (type: {})",
                clientGroup.getClientGroupName(), type)
        );
    }

    private void connectWithRetry(ClientGroup group, WsClientGroupConfig config, int attempt) {
        final String groupName = group.getClientGroupName();
        log.debug("Initiating WS connection for group '{}' (attempt: {})", groupName, attempt);
        try {
            final var req = new WsClientUpgradeRequest(URI.create(config.getUrl()));
            applyAuthenticator(req, config);
            applyHeaders(req, config);
            final var lifecycleWrapper = new ManagedSessionLifecycleListener(
                group,
                config,
                heartbeatManager,
                reconnectManager,
                () -> connectWithRetry(group, config, attempt + 1)
            );
            onClientGroupStart(group, config, req, config.getBusConfig().configureProtocol(req),
                lifecycleWrapper);
        } catch (Exception ex) {
            log.error("Failed to connect WS session for group '{}': {}", groupName,
                ex.getMessage());
            reconnectManager.handleFailure(group, config, attempt,
                () -> connectWithRetry(group, config, attempt + 1));
        }
    }

    private void applyAuthenticator(WsClientUpgradeRequest req, WsClientGroupConfig config) {
        final WsClientAuthenticator authenticator = config.getAuthenticator();
        if (authenticator != null) {
            log.debug("Applying authenticator: {} for group: {}",
                authenticator.getClass().getSimpleName(), config.getPrincipalId());
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

    protected abstract void onClientGroupStart(ClientGroup clientGroup, WsClientGroupConfig config,
                                               WsClientUpgradeRequest req,
                                               WsSessionCodec sessionCodec,
                                               NetworkSessionLifecycleListener<WsClientSession>
                                                   lifecycleListener) throws Exception;

    protected abstract static class AbstractBuilder<B extends AbstractBuilder<B>>
        extends AbstractBaseBuilder<WsClientGroupConfig, B> {
        private WsClientSessionRegistry sessionRegistry = InMemoryWsClientSessionRegistry
            .createDefault();

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
