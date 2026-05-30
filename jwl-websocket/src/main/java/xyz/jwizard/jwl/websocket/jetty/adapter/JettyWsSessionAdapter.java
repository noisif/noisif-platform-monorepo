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
package xyz.jwizard.jwl.websocket.jetty.adapter;

import org.eclipse.jetty.websocket.api.Session;

import xyz.jwizard.jwl.codec.UnsupportedDataTypeException;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.serialization.MessageSerializerException;
import xyz.jwizard.jwl.common.util.concurrent.ConcurrentUtil;
import xyz.jwizard.jwl.net.ws.GenericWsSessionAdapter;
import xyz.jwizard.jwl.websocket.WsSession;

import java.nio.ByteBuffer;

public class JettyWsSessionAdapter extends GenericWsSessionAdapter implements WsSession {
  private final Session session;
  private final EnvelopeSerializer<?> envelopeSerializer;

  public JettyWsSessionAdapter(
      Session session, String principalId, EnvelopeSerializer<?> envelopeSerializer) {
    super(principalId, envelopeSerializer);
    this.session = session;
    this.envelopeSerializer = envelopeSerializer;
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
  public void sendAdapted(byte[] payload) {
    if (isClosed()) {
      log.debug("Skipping sendAdapted - session is closed, sessionId: {}", sessionId);
      return;
    }
    try {
      envelopeSerializer.acceptRaw(payload, this);
    } catch (UnsupportedDataTypeException | MessageSerializerException ex) {
      log.error("Message error for RAW adaptation: {}", ex.getMessage());
    } catch (Exception ex) {
      log.error("Unexpected error during RAW adaptation, sessionId: {}", sessionId, ex);
    }
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
