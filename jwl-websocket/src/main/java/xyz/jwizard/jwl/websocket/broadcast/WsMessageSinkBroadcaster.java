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
package xyz.jwizard.jwl.websocket.broadcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.OpCode;

public class WsMessageSinkBroadcaster implements WsBroadcaster {
  private static final Logger LOG = LoggerFactory.getLogger(WsMessageSinkBroadcaster.class);

  private final WsMessageSink sink;
  private final EnvelopeSerializer<?> serializer;

  public WsMessageSinkBroadcaster(WsMessageSink sink, EnvelopeSerializer<?> serializer) {
    this.sink = sink;
    this.serializer = serializer;
  }

  @Override
  public void broadcast(OpCode op, Object payload) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Broadcasting byte message globally, OP: {}", op);
    }
    sink.payloadAll(serializer.serializeEnvelopeAsBytes(op, payload));
  }

  @Override
  public void broadcast(WsTopic topic, OpCode op, Object payload) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Broadcasting byte message to topic: [{}], OP: {}", topic, op);
    }
    sink.payload(topic, serializer.serializeEnvelopeAsBytes(op, payload));
  }

  @Override
  public void broadcastRaw(byte[] payload) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Broadcasting RAW bytes globally (size: {} bytes)", payload.length);
    }
    sink.payloadAll(payload);
  }

  @Override
  public void broadcastRaw(WsTopic topic, byte[] payload) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Broadcasting RAW bytes to topic: [{}] (size: {} bytes)", topic, payload.length);
    }
    sink.payload(topic, payload);
  }
}
