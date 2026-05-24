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
