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
package xyz.noisif.nsl.netclient.websocket.jetty.adapter;

import org.eclipse.jetty.websocket.api.Session;

import xyz.noisif.nsl.common.util.concurrent.ConcurrentUtil;
import xyz.noisif.nsl.net.ws.GenericWsSessionAdapter;
import xyz.noisif.nsl.netclient.group.ClientGroup;
import xyz.noisif.nsl.netclient.websocket.WsClientSession;
import xyz.noisif.nsl.netclient.websocket.group.codec.WsSessionCodec;

import java.nio.ByteBuffer;

public class JettyWsClientSessionAdapter extends GenericWsSessionAdapter
    implements WsClientSession {
  private final ClientGroup clientGroup;
  private final Session session;
  private final WsSessionCodec sessionCodec;

  public JettyWsClientSessionAdapter(
      ClientGroup clientGroup, Session session, String principalId, WsSessionCodec sessionCodec) {
    super(principalId, sessionCodec);
    this.clientGroup = clientGroup;
    this.session = session;
    this.sessionCodec = sessionCodec;
  }

  @Override
  public ClientGroup getGroup() {
    return clientGroup;
  }

  @Override
  public void sendObject(Object payload) {
    sessionCodec.sendObject(payload, this);
  }

  @Override
  public <T> T parse(byte[] payload, Class<T> type) {
    return sessionCodec.parse(payload, type);
  }

  @Override
  public <T> T parse(String payload, Class<T> type) {
    return sessionCodec.parse(payload, type);
  }

  @Override
  protected void onSend(String message) {
    ConcurrentUtil.await(cb -> session.sendText(message, new JettyCallbackAdapter(cb)));
  }

  @Override
  protected void onSend(byte[] message) {
    ConcurrentUtil.await(
        cb -> session.sendBinary(ByteBuffer.wrap(message), new JettyCallbackAdapter(cb)));
  }

  @Override
  protected void onClose(int code, String reason) {
    ConcurrentUtil.await(cb -> session.close(code, reason, new JettyCallbackAdapter(cb)));
  }

  @Override
  public boolean isClosed() {
    return !session.isOpen();
  }
}
