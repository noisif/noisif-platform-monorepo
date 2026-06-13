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
package xyz.noisif.nsl.net.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.codec.EncodedPayloadVisitor;
import xyz.noisif.nsl.codec.UnifiedMessageCodec;
import xyz.noisif.nsl.codec.UnsupportedDataTypeException;
import xyz.noisif.nsl.codec.envelope.MessageEnvelope;
import xyz.noisif.nsl.codec.envelope.OpCode;
import xyz.noisif.nsl.codec.serialization.MessageSerializerException;
import xyz.noisif.nsl.common.util.io.RunnableWithException;
import xyz.noisif.nsl.net.CloseCode;
import xyz.noisif.nsl.net.NetworkSender;
import xyz.noisif.nsl.net.envelope.EnvelopeSession;

import java.util.UUID;
import java.util.function.Function;

public abstract class GenericWsSessionAdapter
    implements NetworkSender, EncodedPayloadVisitor, EnvelopeSession {
  private static final String UNKNOWN_ENVELOPE = "UNKNOWN";

  protected final Logger log = LoggerFactory.getLogger(getClass());
  protected final String sessionId;
  protected final String principalId;
  protected final UnifiedMessageCodec messageCodec;

  protected GenericWsSessionAdapter(String principalId, UnifiedMessageCodec messageCodec) {
    sessionId = UUID.randomUUID().toString();
    this.principalId = principalId;
    this.messageCodec = messageCodec;
  }

  @Override
  public final void send(byte[] message) {
    sendSafely(
        () ->
            log.trace(
                "Sending binary message, sessionId: {}, size: {} bytes", sessionId, message.length),
        () -> onSend(message));
  }

  @Override
  public final void send(String message) {
    sendSafely(
        () ->
            log.trace(
                "Sending text message, sessionId: {}, size: {} chars", sessionId, message.length()),
        () -> onSend(message));
  }

  @Override
  public final void sendEnvelope(OpCode opCode, Object data) {
    final String opName = opCode != null ? opCode.getName() : UNKNOWN_ENVELOPE;
    if (isClosed()) {
      log.debug("Skipping send: session is closed, sessionId: {}, OP: {}", sessionId, opName);
      return;
    }
    log.debug(
        "Preparing {} envelope, sessionId: {}, OP: {} (ID: {})",
        messageCodec.getFormat().getFormatName(),
        sessionId,
        opName,
        opCode != null ? opCode.getCode() : "null");
    try {
      messageCodec.serializeAndAcceptEnvelope(opCode, data, this);
    } catch (UnsupportedDataTypeException | MessageSerializerException ex) {
      log.error(
          "Message error for {}: {}", messageCodec.getFormat().getFormatName(), ex.getMessage());
    } catch (Exception ex) {
      log.error("Unexpected error during processing, sessionId: {}, OP: {}", sessionId, opName, ex);
    }
  }

  @Override
  public final void accept(byte[] payload) {
    send(payload);
  }

  @Override
  public final void accept(String payload) {
    send(payload);
  }

  @Override
  public final MessageEnvelope<?> unwrap(byte[] payload, Function<Integer, Class<?>> typeResolver) {
    return messageCodec.unwrap(payload, typeResolver);
  }

  @Override
  public final MessageEnvelope<?> unwrap(String payload, Function<Integer, Class<?>> typeResolver) {
    return messageCodec.unwrap(payload, typeResolver);
  }

  @Override
  public final String getSessionId() {
    return sessionId;
  }

  @Override
  public final String getPrincipalId() {
    return principalId;
  }

  @Override
  public final void close(CloseCode closeCode) {
    if (isClosed()) {
      return;
    }
    log.debug(
        "Initiating session closure, sessionId: {}, status: {}, reason: '{}'",
        sessionId,
        closeCode.getCode(),
        closeCode.getDefaultReason());
    onClose(closeCode.getCode(), closeCode.getDefaultReason());
  }

  protected abstract void onSend(String message) throws Exception;

  protected abstract void onSend(byte[] message) throws Exception;

  protected abstract void onClose(int code, String reason);

  private void sendSafely(Runnable logAction, RunnableWithException transportAction) {
    if (isClosed()) {
      log.trace("Skipping send: session is closed, sessionId: {}", sessionId);
      return;
    }
    if (log.isTraceEnabled()) {
      logAction.run();
    }
    try {
      transportAction.run();
    } catch (Exception ex) {
      log.error(
          "Transport layer failed to deliver message for session {}: {}",
          sessionId,
          ex.getMessage());
    }
  }
}
