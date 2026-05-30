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
package xyz.jwizard.jwl.codec.serialization.raw;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.serialization.MessageSerializer;
import xyz.jwizard.jwl.codec.serialization.MessageSerializerException;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;
import xyz.jwizard.jwl.codec.serialization.TypedMessageSerializer;
import xyz.jwizard.jwl.common.util.CastUtil;

public class RawByteSerializer implements MessageSerializer, TypedMessageSerializer<byte[]> {
  private RawByteSerializer() {}

  public static RawByteSerializer createDefault() {
    return new RawByteSerializer();
  }

  @Override
  public byte[] serializeToBytes(Object value) {
    if (value == null) {
      return new byte[0];
    }
    if (value instanceof byte[] array) {
      return array;
    }
    throw new MessageSerializerException(
        "RawByteSerializer can only handle byte[], but received: " + value.getClass().getName());
  }

  @Override
  public <T> T deserializeFromBytes(byte[] bytes, Class<T> type) {
    if (type.isAssignableFrom(byte[].class)) {
      return CastUtil.unsafeCast(bytes);
    }
    throw new MessageSerializerException(
        "RawByteSerializer can only deserialize to byte[].class, but requested: " + type.getName());
  }

  @Override
  public StandardSerializerFormat getFormat() {
    return StandardSerializerFormat.RAW;
  }

  @Override
  public DataType getCodecDataType() {
    return DataType.BINARY;
  }

  @Override
  public byte[] serializePayload(Object payload) {
    return serializeToBytes(payload);
  }

  @Override
  public <T> T deserializePayload(byte[] payload, Class<T> type) {
    return deserializeFromBytes(payload, type);
  }

  @Override
  public void serializeAndAccept(Object payload, EncodedPayloadVisitor visitor) {
    visitor.accept(serializePayload(payload));
  }
}
