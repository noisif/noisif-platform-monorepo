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
package xyz.noisif.nsl.codec.serialization.json.jackson.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.codec.serialization.json.spi.JsonSpiTypeSerializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.io.Serial;

public class JacksonSpiTypeSerializerModule<T> extends SimpleModule {
  @Serial private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(JacksonSpiTypeSerializerModule.class);

  private final transient JsonSpiTypeSerializer<T> serializer;

  private JacksonSpiTypeSerializerModule(JsonSpiTypeSerializer<T> serializer) {
    this.serializer = serializer;
    createSerializer();
  }

  public static <T> JacksonSpiTypeSerializerModule<T> create(JsonSpiTypeSerializer<T> serializer) {
    return new JacksonSpiTypeSerializerModule<>(serializer);
  }

  private void createSerializer() {
    final Class<T> targetType = serializer.getTargetType();
    addSerializer(targetType, new JacksonSpiValueSerializer());
    LOG.debug(
        "Registered serializer {} for type {} into Jackson",
        serializer.getClass().getSimpleName(),
        targetType.getSimpleName());
  }

  private class JacksonSpiValueSerializer extends ValueSerializer<T> {
    @Override
    public void serialize(T value, JsonGenerator gen, SerializationContext context)
        throws JacksonException {
      try {
        serializer.serialize(value, new JacksonSpiWriterAdapter(gen));
      } catch (Exception ex) {
        if (ex instanceof JacksonException jex) {
          throw jex;
        }
        throw new RuntimeException(
            "Failed to serialize " + serializer.getTargetType().getSimpleName(), ex);
      }
    }
  }
}
