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
