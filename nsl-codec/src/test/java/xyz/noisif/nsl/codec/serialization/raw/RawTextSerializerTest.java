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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import xyz.noisif.nsl.codec.DataType;
import xyz.noisif.nsl.codec.EncodedPayloadVisitor;
import xyz.noisif.nsl.codec.serialization.MessageSerializerException;
import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;

import java.nio.charset.StandardCharsets;

@ExtendWith(MockitoExtension.class)
class RawTextSerializerTest {
  private final RawTextSerializer serializer = RawTextSerializer.createDefault();

  @Mock private EncodedPayloadVisitor visitorMock;

  @Test
  @DisplayName("should return the same string on serialization")
  void shouldSerializeRawString() {
    // given
    final String input = "hello noisif text";
    // when
    final String result = serializer.serializePayload(input);
    // then
    assertThat(result).isSameAs(input);
  }

  @Test
  @DisplayName("should return empty string when serializing null")
  void shouldSerializeNullAsEmptyString() {
    // when
    final String result = serializer.serializePayload(null);
    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("should throw exception when trying to serialize non-string object")
  void shouldThrowOnInvalidSerializationType() {
    // given
    final Integer invalidInput = 12345;
    // then
    assertThatThrownBy(() -> serializer.serializePayload(invalidInput))
        .isInstanceOf(MessageSerializerException.class)
        .hasMessageContaining("RawStringSerializer can only handle String");
  }

  @Test
  @DisplayName("should return the same string on deserialization to String.class")
  void shouldDeserializeRawString() {
    // given
    final String input = "test payload";
    // when
    final String result = serializer.deserializePayload(input, String.class);
    // then
    assertThat(result).isSameAs(input);
  }

  @Test
  @DisplayName("should throw exception when requesting deserialization to type other than String")
  void shouldThrowOnInvalidDeserializationType() {
    // given
    final String input = "some data";
    // then
    assertThatThrownBy(() -> serializer.deserializePayload(input, Integer.class))
        .isInstanceOf(MessageSerializerException.class)
        .hasMessageContaining("RawStringSerializer can only deserialize to String.class");
  }

  @Test
  @DisplayName("should serialize string to UTF-8 byte array using fallback method")
  void shouldSerializeToBytesFallback() {
    // given
    final String input = "hello utf8 ąćęł";
    // when
    final byte[] result = serializer.serializeToBytes(input);
    // then
    assertThat(result).isEqualTo(input.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("should deserialize UTF-8 byte array to String using fallback method")
  void shouldDeserializeFromBytesFallback() {
    // given
    final String expected = "hello utf8 ąćęł";
    final byte[] inputBytes = expected.getBytes(StandardCharsets.UTF_8);
    // when
    final String result = serializer.deserializeFromBytes(inputBytes, String.class);
    // then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  @DisplayName("should return correct format")
  void shouldReturnRawFormat() {
    assertThat(serializer.getFormat()).isEqualTo(StandardSerializerFormat.RAW);
  }

  @Test
  @DisplayName("should return TEXT data type")
  void shouldReturnTextDataType() {
    assertThat(serializer.getCodecDataType()).isEqualTo(DataType.TEXT);
  }

  @Test
  @DisplayName("should pass raw string directly to visitor")
  void shouldDelegateTextOperations() {
    // given
    final RawTextSerializer serializer = RawTextSerializer.createDefault();
    final String inputPayload = "Hello NOISIF RAW Mode!";
    // when
    serializer.serializeAndAccept(inputPayload, visitorMock);
    // then
    assertThat(serializer.getCodecDataType()).isEqualTo(DataType.TEXT);
    verify(visitorMock).accept(inputPayload);
  }
}
