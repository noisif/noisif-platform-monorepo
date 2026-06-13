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

import xyz.noisif.nsl.codec.serialization.SerializerFormat;
import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;
import xyz.noisif.nsl.common.util.StringUtil;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.io.OutputStream;

public class JacksonSerializer implements JsonSerializer {
  private final ObjectMapper objectMapper;

  private JacksonSerializer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public static JacksonSerializer createDefaultStrictMapper() {
    final ObjectMapper mapper =
        JsonMapper.builder()
            // error when a field required by the constructor/record is missing in the
            // JSON
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            // error when the JSON contains properties that do not exist in our class
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // prevents setting null for primitive types (int, boolean, etc.)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();
    return new JacksonSerializer(mapper);
  }

  // for loosely coupled service as queues (RabbitMQ, Kafka)
  public static JacksonSerializer createLenientForMessaging() {
    final ObjectMapper mapper =
        JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();
    return new JacksonSerializer(mapper);
  }

  @Override
  public String serialize(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JacksonException ex) {
      throw new JsonSerializerException(getCleanMessage(ex), ex);
    }
  }

  @Override
  public void serializeToStream(Object value, OutputStream out) {
    try {
      objectMapper.writeValue(out, value);
    } catch (JacksonException ex) {
      throw new JsonSerializerException(getCleanMessage(ex), ex);
    }
  }

  @Override
  public <T> T deserializeFromStream(InputStream input, Class<T> type) {
    try {
      return objectMapper.readValue(input, type);
    } catch (JacksonException ex) {
      throw new JsonSerializerException(getCleanMessage(ex), ex);
    }
  }

  @Override
  public <T> T deserialize(String payload, Class<T> type) {
    try {
      return objectMapper.readValue(payload, type);
    } catch (JacksonException ex) {
      throw new JsonSerializerException(getCleanMessage(ex), ex);
    }
  }

  @Override
  public <T> T convert(Object source, Class<T> type) {
    try {
      return objectMapper.convertValue(source, type);
    } catch (IllegalArgumentException ex) {
      throw new JsonSerializerException(
          "Failed to convert payload data to expected type: " + type.getSimpleName(), ex);
    }
  }

  @Override
  public byte[] serializeToBytes(Object value) {
    try {
      return objectMapper.writeValueAsBytes(value);
    } catch (JacksonException ex) {
      throw new JsonSerializerException(getCleanMessage(ex), ex);
    }
  }

  @Override
  public <T> T deserializeFromBytes(byte[] bytes, Class<T> type) {
    try {
      return objectMapper.readValue(bytes, type);
    } catch (JacksonException ex) {
      throw new JsonSerializerException(getCleanMessage(ex), ex);
    }
  }

  @Override
  public SerializerFormat getFormat() {
    return StandardSerializerFormat.JSON;
  }

  private String getCleanMessage(JacksonException ex) {
    final String exStr = StringUtil.splitAndGetFirst(ex.getOriginalMessage().trim(), ';');
    return exStr == null ? "" : exStr;
  }
}
