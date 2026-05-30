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
package xyz.jwizard.jwl.netclient.websocket.group.codec;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.MessageEnvelope;
import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.codec.serialization.SerializerFormat;

import java.util.function.Function;

public class WsEnvelopeSessionCodec implements WsSessionCodec {
  private final EnvelopeSerializer<?> serializer;

  public WsEnvelopeSessionCodec(EnvelopeSerializer<?> serializer) {
    this.serializer = serializer;
  }

  @Override
  public WsSessionCodecMode getCurrentMode() {
    return WsSessionCodecMode.ENVELOPE_MESSAGE;
  }

  @Override
  public void serializeAndAcceptEnvelope(
      OpCode opCode, Object data, EncodedPayloadVisitor visitor) {
    serializer.serializeAndAcceptEnvelope(opCode, data, visitor);
  }

  @Override
  public SerializerFormat getBaseFormat() {
    return serializer.getBaseFormat();
  }

  @Override
  public MessageEnvelope<?> unwrap(byte[] payload, Function<Integer, Class<?>> typeResolver) {
    return serializer.unwrap(payload, typeResolver);
  }

  @Override
  public MessageEnvelope<?> unwrap(String payload, Function<Integer, Class<?>> typeResolver) {
    return serializer.unwrap(payload, typeResolver);
  }

  @Override
  public DataType getCodecDataType() {
    return serializer.getCodecDataType();
  }
}
