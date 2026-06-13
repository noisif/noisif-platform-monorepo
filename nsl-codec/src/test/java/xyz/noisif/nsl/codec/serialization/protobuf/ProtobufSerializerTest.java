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
package xyz.noisif.nsl.codec.serialization.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.MessageLite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import xyz.noisif.nsl.codec.DataType;
import xyz.noisif.nsl.codec.EncodedPayloadVisitor;
import xyz.noisif.nsl.codec.serialization.SerializerFormat;
import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;
import xyz.noisif.nsl.common.reflect.ClassScanner;

import java.io.ByteArrayInputStream;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class ProtobufSerializerTest {
  private ProtobufSerializer serializer;

  @Mock private ClassScanner scannerMock;
  @Mock private EncodedPayloadVisitor visitorMock;
  @Mock private MessageLite messageLiteMock;

  @BeforeEach
  void setUp() {
    // given
    when(scannerMock.getSubtypesOf(MessageLite.class))
        .thenReturn(Set.of(TestMessage.class, ComplexMessage.class));
    // when
    serializer = ProtobufSerializer.createDefault(scannerMock);
  }

  @Test
  @DisplayName("should register test message and handle round-trip serialization")
  void shouldHandleSerialization() {
    // given
    final TestMessage message =
        TestMessage.newBuilder().setId(100).setValue("test-content").build();
    // when
    final byte[] bytes = serializer.serializeToBytes(message);
    final TestMessage result = serializer.deserializeFromBytes(bytes, TestMessage.class);
    // then
    assertThat(result.getId()).isEqualTo(100);
    assertThat(result.getValue()).isEqualTo("test-content");
  }

  @Test
  @DisplayName("should handle stream deserialization for test message")
  void shouldHandleStream() {
    // given
    final TestMessage message = TestMessage.newBuilder().setId(1).build();
    final ByteArrayInputStream in = new ByteArrayInputStream(message.toByteArray());
    // when
    final TestMessage result = serializer.deserializeFromStream(in, TestMessage.class);
    // then
    assertThat(result.getId()).isEqualTo(1);
  }

  @Test
  @DisplayName("should fail when type was not part of the initial scan")
  void shouldFailOnUnregisteredType() {
    // given
    final byte[] data = new byte[0];
    // when & then
    assertThatThrownBy(() -> serializer.deserializeFromBytes(data, OtherTestMessage.class))
        .isInstanceOf(ProtobufSerializerException.class)
        .hasMessageContaining("Type not registered");
  }

  @Test
  @DisplayName("should return correct format for protobuf")
  void shouldReturnFormat() {
    // when
    final SerializerFormat format = serializer.getFormat();
    // then
    assertThat(format).isEqualTo(StandardSerializerFormat.PROTOBUF);
  }

  @Test
  @DisplayName("should handle composition where one proto class uses another")
  void shouldHandleCrossProtoComposition() {
    // given
    final TestMessage inner = TestMessage.newBuilder().setId(99).setValue("nested-content").build();
    final ComplexMessage complex =
        ComplexMessage.newBuilder().setDescription("root-container").setCoreData(inner).build();
    // when
    final byte[] bytes = serializer.serializeToBytes(complex);
    final ComplexMessage result = serializer.deserializeFromBytes(bytes, ComplexMessage.class);
    // then
    assertThat(result.getDescription()).isEqualTo("root-container");
    assertThat(result.getCoreData().getId()).isEqualTo(99);
    assertThat(result.getCoreData().getValue()).isEqualTo("nested-content");
  }

  @Test
  @DisplayName("should serialize MessageLite to bytes and pass to visitor")
  void shouldDelegateBinaryOperations() {
    // given
    given(scannerMock.getSubtypesOf(MessageLite.class)).willReturn(Set.of());
    final ProtobufSerializer serializer = ProtobufSerializer.createDefault(scannerMock);
    final byte[] expectedBytes = {0x0A, 0x0B, 0x0C};
    given(messageLiteMock.toByteArray()).willReturn(expectedBytes);
    // when
    serializer.serializeAndAccept(messageLiteMock, visitorMock);
    // then
    assertThat(serializer.getCodecDataType()).isEqualTo(DataType.BINARY);
    verify(visitorMock).accept(expectedBytes);
  }
}

abstract class OtherTestMessage implements MessageLite {}
