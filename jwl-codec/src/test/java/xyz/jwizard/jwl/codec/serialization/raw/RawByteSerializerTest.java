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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.serialization.MessageSerializerException;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;
import xyz.jwizard.jwl.common.util.StringUtil;

@ExtendWith(MockitoExtension.class)
class RawByteSerializerTest {
  private final RawByteSerializer serializer = RawByteSerializer.createDefault();

  @Mock private EncodedPayloadVisitor visitorMock;

  @Test
  @DisplayName("should return the same byte array on serialization")
  void shouldSerializeRawBytes() {
    // given
    final byte[] input = StringUtil.getBytes("hello jwizard");
    // when
    final byte[] result = serializer.serializeToBytes(input);
    // then
    assertThat(result).isSameAs(input);
  }

  @Test
  @DisplayName("should return empty array when serializing null")
  void shouldSerializeNullAsEmptyArray() {
    // when
    final byte[] result = serializer.serializeToBytes(null);
    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("should throw exception when trying to serialize non-byte-array object")
  void shouldThrowOnInvalidSerializationType() {
    // given
    final String invalidInput = "I am not a byte array";
    // then
    assertThatThrownBy(() -> serializer.serializeToBytes(invalidInput))
        .isInstanceOf(MessageSerializerException.class)
        .hasMessageContaining("RawByteSerializer can only handle byte[]");
  }

  @Test
  @DisplayName("should return the same byte array on deserialization to byte[].class")
  void shouldDeserializeRawBytes() {
    // given
    final byte[] input = {0x01, 0x02, 0x03};
    // when
    final byte[] result = serializer.deserializeFromBytes(input, byte[].class);
    // then
    assertThat(result).isSameAs(input);
  }

  @Test
  @DisplayName("should throw exception when requesting deserialization to type other than byte[]")
  void shouldThrowOnInvalidDeserializationType() {
    // given
    final byte[] input = new byte[0];
    // then
    assertThatThrownBy(() -> serializer.deserializeFromBytes(input, String.class))
        .isInstanceOf(MessageSerializerException.class)
        .hasMessageContaining("RawByteSerializer can only deserialize to byte[].class");
  }

  @Test
  @DisplayName("should return correct format")
  void shouldReturnRawFormat() {
    assertThat(serializer.getFormat()).isEqualTo(StandardSerializerFormat.RAW);
  }

  @Test
  @DisplayName("should pass raw byte array directly to visitor")
  void shouldDelegateBinaryOperations() {
    // given
    final RawByteSerializer serializer = RawByteSerializer.createDefault();
    final byte[] inputPayload = {0x01, 0x02, 0x03};
    // when
    serializer.serializeAndAccept(inputPayload, visitorMock);
    // then
    assertThat(serializer.getCodecDataType()).isEqualTo(DataType.BINARY);
    verify(visitorMock).accept(inputPayload);
  }
}
