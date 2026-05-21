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
package xyz.jwizard.jwl.netclient.websocket.jetty;

import java.util.concurrent.Executors;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.net.lifecycle.NetworkSessionLifecycleListener;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.websocket.GenericWsClient;
import xyz.jwizard.jwl.netclient.websocket.WsClientSession;
import xyz.jwizard.jwl.netclient.websocket.WsClientUpgradeRequest;
import xyz.jwizard.jwl.netclient.websocket.group.WsClientGroupConfig;
import xyz.jwizard.jwl.netclient.websocket.group.codec.WsSessionCodec;
import xyz.jwizard.jwl.netclient.websocket.jetty.adapter.JettyWsClientListenerAdapter;

public class JettyWsClient extends GenericWsClient {
    private WebSocketClient jettyWsClient;

    private JettyWsClient(AbstractBuilder<?> builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void onStart() throws Exception {
        final HttpClient jettyHttpClient = new HttpClient();
        jettyHttpClient.setConnectTimeout(connectTimeout.toMillis());

        // virtual threads
        final QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("ws-vt-pool");
        threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
        jettyHttpClient.setExecutor(threadPool);

        jettyWsClient = new WebSocketClient(jettyHttpClient);

        jettyWsClient.start();
        super.onStart();
    }

    @Override
    protected void onClientGroupStart(ClientGroup clientGroup, WsClientGroupConfig config,
                                      WsClientUpgradeRequest req,
                                      WsSessionCodec sessionCodec,
                                      NetworkSessionLifecycleListener<WsClientSession>
                                          lifecycleListener) throws Exception {
        final ClientUpgradeRequest request = new ClientUpgradeRequest(req.getUri());
        request.setHeaders(req.getHeaders());
        final JettyWsClientListenerAdapter listener = new JettyWsClientListenerAdapter(
            clientGroup,
            config,
            sessionRegistry,
            sessionCodec,
            lifecycleListener
        );
        jettyWsClient.connect(listener, request).get();
    }

    @Override
    protected void onStop() {
        IoUtil.closeQuietly(jettyWsClient, AbstractLifeCycle::stop);
        IoUtil.closeQuietly(jettyWsClient);
        super.onStop();
    }

    public static class Builder extends AbstractBuilder<Builder> {
        private Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public GenericWsClient build() {
            super.validate();
            return new JettyWsClient(this);
        }
    }
}
