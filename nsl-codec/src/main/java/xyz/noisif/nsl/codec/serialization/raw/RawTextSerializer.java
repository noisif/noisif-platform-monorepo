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
package xyz.noisif.nsl.codec.serialization.raw;

import xyz.noisif.nsl.codec.DataType;
import xyz.noisif.nsl.codec.EncodedPayloadVisitor;
import xyz.noisif.nsl.codec.serialization.MessageSerializer;
import xyz.noisif.nsl.codec.serialization.MessageSerializerException;
import xyz.noisif.nsl.codec.serialization.SerializerFormat;
import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;
import xyz.noisif.nsl.codec.serialization.TypedMessageSerializer;
import xyz.noisif.nsl.common.util.CastUtil;

import java.nio.charset.StandardCharsets;

public class RawTextSerializer implements MessageSerializer, TypedMessageSerializer<String> {
  private RawTextSerializer() {}

  public static RawTextSerializer createDefault() {
    return new RawTextSerializer();
  }

  @Override
  public byte[] serializeToBytes(Object value) {
    return serializePayload(value).getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public <T> T deserializeFromBytes(byte[] bytes, Class<T> type) {
    final String str = new String(bytes, StandardCharsets.UTF_8);
    return deserializePayload(str, type);
  }

  @Override
  public SerializerFormat getFormat() {
    return StandardSerializerFormat.RAW;
  }

  @Override
  public String serializePayload(Object payload) {
    if (payload == null) {
      return "";
    }
    if (payload instanceof String str) {
      return str;
    }
    throw new MessageSerializerException(
        "RawStringSerializer can only handle String, but received: "
            + payload.getClass().getName());
  }

  @Override
  public <T> T deserializePayload(String payload, Class<T> type) {
    if (type.isAssignableFrom(String.class)) {
      return CastUtil.unsafeCast(payload);
    }
    throw new MessageSerializerException(
        "RawStringSerializer can only deserialize to String.class, but requested: "
            + type.getName());
  }

  @Override
  public void serializeAndAccept(Object payload, EncodedPayloadVisitor visitor) {
    visitor.accept(serializePayload(payload));
  }

  @Override
  public DataType getCodecDataType() {
    return DataType.TEXT;
  }
}
