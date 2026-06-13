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
package xyz.noisif.nsl.codec;

import xyz.noisif.nsl.codec.envelope.MessageEnvelope;
import xyz.noisif.nsl.codec.envelope.OpCode;
import xyz.noisif.nsl.codec.serialization.SerializerFormat;
import xyz.noisif.nsl.codec.serialization.TypedSerializer;
import xyz.noisif.nsl.codec.serialization.TypedSerializerFormat;

import java.util.function.Function;

public interface UnifiedMessageCodec extends TypedSerializer {
  SerializerFormat getBaseFormat();

  @Override
  default SerializerFormat getFormat() {
    return TypedSerializerFormat.from(getBaseFormat(), getCodecDataType());
  }

  void serializeAndAcceptEnvelope(OpCode opCode, Object data, EncodedPayloadVisitor visitor);

  MessageEnvelope<?> unwrap(byte[] payload, Function<Integer, Class<?>> typeResolver);

  MessageEnvelope<?> unwrap(String payload, Function<Integer, Class<?>> typeResolver);
}
