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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import xyz.noisif.nsl.codec.DataType;
import xyz.noisif.nsl.codec.EncodedPayloadVisitor;
import xyz.noisif.nsl.codec.envelope.TestOpCode;
import xyz.noisif.nsl.codec.serialization.json.JsonSerializer;

import java.nio.charset.StandardCharsets;

@ExtendWith(MockitoExtension.class)
class JsonEnvelopeSerializersTest {
  @Mock private JsonSerializer jsonSerializerMock;
  @Mock private EncodedPayloadVisitor visitorMock;
  private JsonTextEnvelopeSerializer textSerializer;
  private JsonBinaryEnvelopeSerializer binarySerializer;

  @BeforeEach
  void setUp() {
    textSerializer = JsonTextEnvelopeSerializer.createDefault(jsonSerializerMock);
    binarySerializer = JsonBinaryEnvelopeSerializer.createDefault(jsonSerializerMock);
  }

  @Test
  @DisplayName(
      "JsonTextEnvelopeSerializer should dispatch to accept(String) "
          + "and convert raw bytes to String")
  void testJsonTextSerializer() {
    // given
    final String mockJson = "{\"op\":65636,\"data\":\"hello\"}";
    when(jsonSerializerMock.serialize(any())).thenReturn(mockJson);
    // when
    assertThat(textSerializer.getCodecDataType()).isEqualTo(DataType.TEXT);
    textSerializer.serializeAndAcceptEnvelope(TestOpCode.USER_DATA, "hello", visitorMock);
    // then
    verify(visitorMock).accept(mockJson);
    // when
    final byte[] rawInput = "raw_test".getBytes(StandardCharsets.UTF_8);
    textSerializer.acceptRaw(rawInput, visitorMock);
    // then
    verify(visitorMock).accept("raw_test"); // Bytes are converted to String!
    verifyNoMoreInteractions(visitorMock);
  }

  @Test
  @DisplayName(
      "JsonBinaryEnvelopeSerializer should dispatch to accept(byte[]) "
          + "and pass raw bytes directly")
  void testJsonBinarySerializer() {
    // given
    final byte[] mockBytes = {1, 2, 3};
    when(jsonSerializerMock.serializeToBytes(any())).thenReturn(mockBytes);
    // when
    assertThat(binarySerializer.getCodecDataType()).isEqualTo(DataType.BINARY);
    binarySerializer.serializeAndAcceptEnvelope(TestOpCode.USER_DATA, "hello", visitorMock);
    // then
    verify(visitorMock).accept(mockBytes);
    // when
    final byte[] rawInput = {9, 8, 7};
    binarySerializer.acceptRaw(rawInput, visitorMock);
    // then
    verify(visitorMock).accept(rawInput);
    verifyNoMoreInteractions(visitorMock);
  }
}
