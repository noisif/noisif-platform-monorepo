/*
 * Copyright (c) 2022-2026 JWizard. All Rights Reserved.
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
package xyz.jwizard.jwl.websocket.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.websocket.WsServer;

import java.util.concurrent.Executors;

public class JettyWsServer extends WsServer {
  private static final long SHUTDOWN_TIMEOUT_MS = 10000;

  private Server server;
  private ServerConnector connector;

  protected JettyWsServer(AbstractBuilder<?> builder) {
    super(builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected void onStart() throws Exception {
    final QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
    queuedThreadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
    queuedThreadPool.setName("ws-vt-pool");

    server = new Server(queuedThreadPool);
    server.setStopTimeout(SHUTDOWN_TIMEOUT_MS);
    server.setStopAtShutdown(false);

    connector = new ServerConnector(server);
    connector.setPort(port);
    server.addConnector(connector);

    final WebSocketUpgradeHandler wsHandler =
        WebSocketUpgradeHandler.from(
            server,
            container -> {
              container.setIdleTimeout(idleTimeout);
              container.setMaxTextMessageSize(maxMessageSize);
              container.addMapping(
                  path,
                  new JettyWsCreator(
                      lifecycleListener,
                      busListener,
                      sessionRegistry,
                      authenticator,
                      authFailureHandler,
                      rateLimiter,
                      serializerResolver));
            });
    server.setHandler(wsHandler);

    server.start();
    log.info(
        "WebSocket server started successfully at '{}' with {}ms shutdown timeout",
        path,
        SHUTDOWN_TIMEOUT_MS);
  }

  @Override
  protected void onStop() {
    IoUtil.closeQuietly(server, AbstractLifeCycle::stop);
  }

  @Override
  public int getLocalPort() {
    Assert.state(connector != null && connector.isRunning(), "Connector is not running");
    return connector.getLocalPort();
  }

  public static class Builder extends AbstractBuilder<Builder> {
    private Builder() {}

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public WsServer build() {
      validate();
      return new JettyWsServer(this);
    }
  }
}
