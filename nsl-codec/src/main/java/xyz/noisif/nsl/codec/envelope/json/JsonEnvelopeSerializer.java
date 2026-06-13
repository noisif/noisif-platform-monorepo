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

import xyz.noisif.nsl.codec.envelope.EnvelopeSerializer;
import xyz.noisif.nsl.codec.envelope.MessageEnvelope;
import xyz.noisif.nsl.codec.envelope.OpCode;
import xyz.noisif.nsl.codec.serialization.SerializerFormat;
import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;
import xyz.noisif.nsl.codec.serialization.json.JsonSerializer;
import xyz.noisif.nsl.codec.serialization.json.JsonSerializerException;

import java.util.Map;
import java.util.function.Function;

public abstract class JsonEnvelopeSerializer<T> implements EnvelopeSerializer<T> {
  protected final JsonSerializer serializer;

  protected JsonEnvelopeSerializer(JsonSerializer serializer) {
    this.serializer = serializer;
  }

  @Override
  public SerializerFormat getBaseFormat() {
    return StandardSerializerFormat.JSON;
  }

  @Override
  public MessageEnvelope<?> unwrap(byte[] payload, Function<Integer, Class<?>> typeResolver) {
    final Map<?, ?> tree = serializer.deserializeFromBytes(payload, Map.class);
    return parseMapToEnvelope(tree, typeResolver);
  }

  @Override
  public MessageEnvelope<?> unwrap(String payload, Function<Integer, Class<?>> typeResolver) {
    final Map<?, ?> tree = serializer.deserialize(payload, Map.class);
    return parseMapToEnvelope(tree, typeResolver);
  }

  @Override
  public byte[] serializeEnvelopeAsBytes(OpCode opCode, Object payload) {
    final MessageEnvelope<Object> envelope = new MessageEnvelope<>(opCode.getCode(), payload);
    return serializer.serializeToBytes(envelope);
  }

  @Override
  public String serializeEnvelopeAsString(OpCode opCode, Object payload) {
    final MessageEnvelope<Object> envelope = new MessageEnvelope<>(opCode.getCode(), payload);
    return serializer.serialize(envelope);
  }

  private MessageEnvelope<?> parseMapToEnvelope(
      Map<?, ?> tree, Function<Integer, Class<?>> typeResolver) {
    if (tree == null) {
      throw new JsonSerializerException("Received empty or null payload");
    }
    final Object opRaw = tree.get("op");
    if (!(opRaw instanceof Number opNumber)) {
      throw new JsonSerializerException("Missing or invalid 'op' field in envelope");
    }
    final int op = opNumber.intValue();
    final Class<?> dataType = typeResolver.apply(op);
    if (dataType == null) {
      return new MessageEnvelope<>(op, null); // checked in ActionRouterWsMessageListener
    }
    final Object rawData = tree.get("data");
    Object data = null;
    if (rawData != null && dataType != Void.class) {
      data = serializer.convert(rawData, dataType);
    }
    return new MessageEnvelope<>(op, data);
  }
}
