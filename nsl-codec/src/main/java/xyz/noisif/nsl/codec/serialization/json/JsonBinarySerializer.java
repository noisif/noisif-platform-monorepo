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
package xyz.noisif.nsl.codec.serialization.json;

import xyz.noisif.nsl.codec.DataType;
import xyz.noisif.nsl.codec.EncodedPayloadVisitor;

public class JsonBinarySerializer extends TypedJsonSerializer<byte[]> {
  private final JsonSerializer jsonSerializer;

  private JsonBinarySerializer(JsonSerializer jsonSerializer) {
    this.jsonSerializer = jsonSerializer;
  }

  public static JsonBinarySerializer create(JsonSerializer jsonSerializer) {
    return new JsonBinarySerializer(jsonSerializer);
  }

  @Override
  public byte[] serializePayload(Object payload) {
    return jsonSerializer.serializeToBytes(payload);
  }

  @Override
  public <T> T deserializePayload(byte[] payload, Class<T> type) {
    return jsonSerializer.deserializeFromBytes(payload, type);
  }

  @Override
  public void serializeAndAccept(Object payload, EncodedPayloadVisitor visitor) {
    visitor.accept(serializePayload(payload));
  }

  @Override
  public DataType getCodecDataType() {
    return DataType.BINARY;
  }
}
