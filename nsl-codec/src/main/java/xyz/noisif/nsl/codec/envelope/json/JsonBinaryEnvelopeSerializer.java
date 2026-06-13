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
package xyz.noisif.nsl.codec.envelope.json;

import xyz.noisif.nsl.codec.DataType;
import xyz.noisif.nsl.codec.EncodedPayloadVisitor;
import xyz.noisif.nsl.codec.envelope.OpCode;
import xyz.noisif.nsl.codec.serialization.json.JsonSerializer;

public class JsonBinaryEnvelopeSerializer extends JsonEnvelopeSerializer<byte[]> {
  private JsonBinaryEnvelopeSerializer(JsonSerializer serializer) {
    super(serializer);
  }

  public static JsonBinaryEnvelopeSerializer createDefault(JsonSerializer serializer) {
    return new JsonBinaryEnvelopeSerializer(serializer);
  }

  @Override
  public DataType getCodecDataType() {
    return DataType.BINARY;
  }

  @Override
  public byte[] serializeForSession(OpCode opCode, Object payload) {
    return serializeEnvelopeAsBytes(opCode, payload);
  }

  @Override
  public void serializeAndAcceptEnvelope(
      OpCode opCode, Object payload, EncodedPayloadVisitor visitor) {
    visitor.accept(serializeForSession(opCode, payload));
  }

  @Override
  public void acceptRaw(byte[] rawPayload, EncodedPayloadVisitor visitor) {
    visitor.accept(rawPayload);
  }
}
