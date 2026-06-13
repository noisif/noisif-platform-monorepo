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
package xyz.noisif.nsl.codec.envelope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.noisif.nsl.codec.DataType;
import xyz.noisif.nsl.codec.EncodedPayloadVisitor;
import xyz.noisif.nsl.codec.UnsupportedDataTypeException;
import xyz.noisif.nsl.codec.serialization.SerializerFormat;
import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;
import xyz.noisif.nsl.codec.serialization.TypedSerializerFormat;

import java.util.function.Function;

class TypedSerializerFormatTest {
  @Test
  @DisplayName("should properly combine base format and data type into full format string")
  void shouldCombineFormatAndDataType() {
    // given
    final SerializerFormat base = StandardSerializerFormat.JSON;
    final DataType type = DataType.TEXT;
    // when
    final TypedSerializerFormat format = TypedSerializerFormat.from(base, type);
    // then
    assertThat(format.getFormatName()).isEqualTo("json+text");
    assertThat(format.toString()).isEqualTo("json+text");
    assertThat(format.baseFormat()).isEqualTo(base);
    assertThat(format.dataType()).isEqualTo(type);
  }

  @Test
  @DisplayName("should throw exception on unsupported default text methods")
  void shouldThrowOnDefaultInterfaceMethods() {
    // given
    EnvelopeSerializer<byte[]> defaultSerializer =
        new EnvelopeSerializer<>() {
          @Override
          public SerializerFormat getBaseFormat() {
            return StandardSerializerFormat.PROTOBUF;
          }

          @Override
          public DataType getCodecDataType() {
            return DataType.BINARY;
          }

          @Override
          public byte[] serializeForSession(OpCode opCode, Object payload) {
            return new byte[0];
          }

          @Override
          public byte[] serializeEnvelopeAsBytes(OpCode opCode, Object payload) {
            return new byte[0];
          }

          @Override
          public void serializeAndAcceptEnvelope(
              OpCode opCode, Object payload, EncodedPayloadVisitor visitor) {}

          @Override
          public void acceptRaw(byte[] rawPayload, EncodedPayloadVisitor visitor) {}

          @Override
          public MessageEnvelope<?> unwrap(
              byte[] payload, Function<Integer, Class<?>> typeResolver) {
            return null;
          }
        };
    // then
    assertThatThrownBy(
            () -> defaultSerializer.serializeEnvelopeAsString(TestOpCode.USER_DATA, "test"))
        .isInstanceOf(UnsupportedDataTypeException.class)
        .hasMessageContaining("Text frames are not supported by protobuf+binary");
    assertThatThrownBy(() -> defaultSerializer.unwrap("{}", id -> String.class))
        .isInstanceOf(UnsupportedDataTypeException.class)
        .hasMessageContaining("Text frames are not supported by protobuf+binary");
  }
}
