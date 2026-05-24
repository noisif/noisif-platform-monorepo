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
package xyz.jwizard.jwl.http.jetty;

import java.util.concurrent.Executors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.http.HttpRequestHandler;
import xyz.jwizard.jwl.http.HttpServer;
import xyz.jwizard.jwl.http.jetty.adapter.JettyHttpRequestHandlerAdapter;

public class JettyHttpServer extends HttpServer {
    private static final long SHUTDOWN_TIMEOUT_MS = 10000;

    private Server server;
    private ServerConnector connector;

    protected JettyHttpServer(AbstractBuilder<?> builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected final void onStart() throws Exception {
        final HttpRequestHandler httpRequestHandler = prepareRequestHandler();

        final QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
        queuedThreadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
        queuedThreadPool.setName("http-vt-pool");

        server = new Server(queuedThreadPool);
        server.setStopTimeout(SHUTDOWN_TIMEOUT_MS);
        server.setStopAtShutdown(false);

        connector = new ServerConnector(server);
        connector.setPort(port);

        server.addConnector(connector);
        server.setHandler(new JettyHttpRequestHandlerAdapter(httpRequestHandler));

        server.start();
        log.info("HTTP server started successfully with {}ms shutdown timeout",
            SHUTDOWN_TIMEOUT_MS);
    }

    @Override
    protected final void onStop() {
        IoUtil.closeQuietly(server, AbstractLifeCycle::stop);
    }

    @Override
    public final int getLocalPort() {
        Assert.state(connector != null && connector.isRunning(), "Connector is not running");
        return connector.getLocalPort();
    }

    public static class Builder extends AbstractBuilder<Builder> {
        private Builder() {
        }

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
