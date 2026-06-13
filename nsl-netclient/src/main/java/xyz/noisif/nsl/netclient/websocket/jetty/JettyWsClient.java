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
package xyz.noisif.nsl.netclient.websocket.jetty;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import xyz.noisif.nsl.common.util.io.IoUtil;
import xyz.noisif.nsl.net.lifecycle.NetworkSessionLifecycleListener;
import xyz.noisif.nsl.netclient.group.ClientGroup;
import xyz.noisif.nsl.netclient.websocket.GenericWsClient;
import xyz.noisif.nsl.netclient.websocket.WsClientSession;
import xyz.noisif.nsl.netclient.websocket.WsClientUpgradeRequest;
import xyz.noisif.nsl.netclient.websocket.group.WsClientGroupConfig;
import xyz.noisif.nsl.netclient.websocket.group.codec.WsSessionCodec;
import xyz.noisif.nsl.netclient.websocket.jetty.adapter.JettyWsClientListenerAdapter;

import java.util.concurrent.Executors;

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
  protected void onClientGroupStart(
      ClientGroup clientGroup,
      WsClientGroupConfig config,
      WsClientUpgradeRequest req,
      WsSessionCodec sessionCodec,
      NetworkSessionLifecycleListener<WsClientSession> lifecycleListener)
      throws Exception {
    final ClientUpgradeRequest request = new ClientUpgradeRequest(req.getUri());
    request.setHeaders(req.getHeaders());
    final JettyWsClientListenerAdapter listener =
        new JettyWsClientListenerAdapter(
            clientGroup, config, sessionRegistry, sessionCodec, lifecycleListener);
    jettyWsClient.connect(listener, request).get();
  }

  @Override
  protected void onStop() {
    IoUtil.closeQuietly(jettyWsClient, AbstractLifeCycle::stop);
    IoUtil.closeQuietly(jettyWsClient);
    super.onStop();
  }

  public static class Builder extends AbstractBuilder<Builder> {
    private Builder() {}

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
