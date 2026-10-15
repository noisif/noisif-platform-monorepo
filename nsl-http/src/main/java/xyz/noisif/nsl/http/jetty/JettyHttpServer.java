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
package xyz.noisif.nsl.http.jetty;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.common.util.io.IoUtil;
import xyz.noisif.nsl.http.HttpRequestHandler;
import xyz.noisif.nsl.http.HttpServer;
import xyz.noisif.nsl.http.acceptor.ConnectionAcceptor;
import xyz.noisif.nsl.http.acceptor.HttpAcceptorBinding;
import xyz.noisif.nsl.http.jetty.adapter.JettyHttpRequestHandlerAdapter;
import xyz.noisif.nsl.http.jetty.resource.JettyStaticResourceInterceptor;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class JettyHttpServer extends HttpServer {
  private static final long SHUTDOWN_TIMEOUT_MS = 10000;

  private Server server;
  private final Map<ConnectionAcceptor, ServerConnector> connectors = new HashMap<>();

  protected JettyHttpServer(AbstractBuilder<?> builder) {
    super(builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected final void onStart() throws Exception {
    final Map<ConnectionAcceptor, HttpRequestHandler> requestHandlers = prepareRequestHandler();

    final QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
    queuedThreadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
    queuedThreadPool.setName("http-vt-pool");

    server = new Server(queuedThreadPool);
    server.setStopTimeout(SHUTDOWN_TIMEOUT_MS);
    server.setStopAtShutdown(false);

    final ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
    for (final HttpAcceptorBinding binding : bindings.values()) {
      final ConnectionAcceptor acceptor = binding.getConnectionAcceptor();
      final HttpRequestHandler httpRequestHandler = requestHandlers.get(acceptor);
      final String connectorName = acceptor.getConnectionAcceptorName();

      final ServerConnector connector = new ServerConnector(server);
      connector.setName(connectorName);
      connector.setPort(binding.getPort());
      server.addConnector(connector);
      connectors.put(acceptor, connector);

      final ContextHandler context = getContext(binding, httpRequestHandler, connectorName);
      handlerCollection.addHandler(context);
    }
    server.setHandler(handlerCollection);
    server.start();
    log.info("HTTP server started successfully with {}ms shutdown timeout", SHUTDOWN_TIMEOUT_MS);
  }

  private ContextHandler getContext(
      HttpAcceptorBinding binding, HttpRequestHandler handler, String connectorName) {
    Handler targetHandler = new JettyHttpRequestHandlerAdapter(handler);
    if (binding.isStaticContentEnabled()) {
      final String baseDir = binding.getStaticContentBase();
      final URL webrootPath = IoUtil.getResourceUrl(baseDir);
      if (webrootPath != null) {
        log.debug("Static content serving is enabled, base directory: '{}'", baseDir);

        final ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirAllowed(false);
        resourceHandler.setAcceptRanges(true);
        resourceHandler.setWelcomeFiles("index.html");
        resourceHandler.setBaseResourceAsString(webrootPath.toExternalForm());

        final Handler staticContentWrapper =
            new JettyStaticResourceInterceptor(
                resourceHandler, binding.getStaticResourcesInterceptor());
        targetHandler = new Handler.Sequence(staticContentWrapper, targetHandler);
      } else {
        log.warn(
            "Unable to determine static content serving base directory, "
                + "skipping static resource handler initialization");
      }
    }
    final ContextHandler context = new ContextHandler("/");
    context.setVirtualHosts(List.of("@" + connectorName));
    context.setHandler(targetHandler);
    return context;
  }

  @Override
  protected final void onStop() {
    IoUtil.closeQuietly(server, AbstractLifeCycle::stop);
  }

  @Override
  public final int getLocalPort(ConnectionAcceptor connectionAcceptor) {
    final ServerConnector connector = connectors.get(connectionAcceptor);
    Assert.state(connector != null, "No connector registered for acceptor: " + connectionAcceptor);
    Assert.state(
        connector.isRunning(), "Connector is not running for acceptor: " + connectionAcceptor);
    return connector.getLocalPort();
  }

  public static class Builder extends AbstractBuilder<Builder> {
    private Builder() {}

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public HttpServer build() {
      validate();
      return new JettyHttpServer(this);
    }
  }
}
