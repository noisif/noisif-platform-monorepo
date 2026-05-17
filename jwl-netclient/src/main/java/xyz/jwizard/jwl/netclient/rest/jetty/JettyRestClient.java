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
package xyz.jwizard.jwl.netclient.rest.jetty;

import java.util.concurrent.Executors;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.common.util.math.MemSize;
import xyz.jwizard.jwl.common.util.math.MemUnit;
import xyz.jwizard.jwl.net.http.HttpMethod;
import xyz.jwizard.jwl.netclient.rest.GenericRestClient;
import xyz.jwizard.jwl.netclient.rest.jetty.spec.JettyRequestSpec;
import xyz.jwizard.jwl.netclient.rest.spec.RequestSpec;

public class JettyRestClient extends GenericRestClient {
    private final int maxQueuedRequests;
    private final int maxConnectionsPerHost;
    private final int maxHeadersSize;

    private HttpClient jettyClient;

    private JettyRestClient(Builder builder) {
        super(builder);
        maxQueuedRequests = builder.maxQueuedRequests;
        maxConnectionsPerHost = builder.maxConnectionsPerHost;
        maxHeadersSize = builder.maxHeadersSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void onStart() throws Exception {
        log.info("Starting JettyRestClient (maxConnectionsPerHost: {}, maxQueuedRequests: {}, " +
                "maxHeadersSize: {} bytes)", maxConnectionsPerHost, maxQueuedRequests,
            maxHeadersSize);
        jettyClient = new HttpClient();

        jettyClient.setConnectTimeout(connectTimeout.toMillis());
        jettyClient.setFollowRedirects(followRedirects);
        jettyClient.setMaxRedirects(maxRedirects);

        jettyClient.setMaxConnectionsPerDestination(maxConnectionsPerHost);
        jettyClient.setMaxRequestsQueuedPerDestination(maxQueuedRequests);
        jettyClient.setMaxRequestHeadersSize(maxHeadersSize);

        // virtual threads
        final QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("rest-vt-pool");
        threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
        jettyClient.setExecutor(threadPool);

        jettyClient.start();
    }

    @Override
    protected void onStop() {
        IoUtil.closeQuietly(jettyClient, AbstractLifeCycle::stop);
    }

    @Override
    public RequestSpec get(String uri) {
        return request(HttpMethod.GET, uri);
    }

    @Override
    public RequestSpec post(String uri) {
        return request(HttpMethod.POST, uri);
    }

    @Override
    public RequestSpec put(String uri) {
        return request(HttpMethod.PUT, uri);
    }

    @Override
    public RequestSpec patch(String uri) {
        return request(HttpMethod.PATCH, uri);
    }

    @Override
    public RequestSpec delete(String uri) {
        return request(HttpMethod.DELETE, uri);
    }

    @Override
    public RequestSpec request(HttpMethod method, String url) {
        log.trace("Creating new JettyRequestSpec: {} {}", method, url);
        return new JettyRequestSpec(
            jettyClient,
            clientsRegistry,
            url,
            method,
            serializerRegistry,
            scanner
        );
    }

    public static class Builder extends GenericRestClient.AbstractBuilder<Builder> {
        private int maxQueuedRequests = 1024;
        private int maxConnectionsPerHost = 128;
        private int maxHeadersSize = 8192;

        private Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder maxQueuedRequests(int maxQueuedRequests) {
            this.maxQueuedRequests = maxQueuedRequests;
            return this;
        }

        public Builder maxConnectionsPerHost(int maxConnectionsPerHost) {
            this.maxConnectionsPerHost = maxConnectionsPerHost;
            return this;
        }

        public Builder maxHeadersSize(int maxHeadersSize, MemUnit unit) {
            this.maxHeadersSize = MemSize.of(maxHeadersSize, unit);
            return this;
        }

        public Builder maxHeadersSizeBytes(int maxHeadersSize) {
            this.maxHeadersSize = MemSize.of(maxHeadersSize, MemUnit.BYTES);
            return this;
        }

        @Override
        public GenericRestClient build() {
            super.validate();
            Assert.state(maxQueuedRequests > 0, "MaxQueuedRequests must be greater than zero");
            Assert.state(maxConnectionsPerHost > 0,
                "MaxConnectionsPerHost must be greater than zero");
            Assert.state(maxHeadersSize >= 1024,
                "MaxHeadersSize must be at least 1024 bytes (1KB)");
            return new JettyRestClient(this);
        }
    }
}
