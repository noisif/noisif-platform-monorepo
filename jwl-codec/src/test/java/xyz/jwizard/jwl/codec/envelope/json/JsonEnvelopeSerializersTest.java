/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jwizard.jwl.codec.envelope.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.envelope.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.envelope.TestOpCode;
import xyz.jwizard.jwl.codec.serialization.json.JsonSerializer;

@ExtendWith(MockitoExtension.class)
class JsonEnvelopeSerializersTest {
    @Mock
    private JsonSerializer jsonSerializerMock;
    @Mock
    private EncodedPayloadVisitor visitorMock;
    private JsonTextEnvelopeSerializer textSerializer;
    private JsonBinaryEnvelopeSerializer binarySerializer;

    @BeforeEach
    void setUp() {
        textSerializer = JsonTextEnvelopeSerializer.createDefault(jsonSerializerMock);
        binarySerializer = JsonBinaryEnvelopeSerializer.createDefault(jsonSerializerMock);
    }

    @Test
    @DisplayName("JsonTextEnvelopeSerializer should dispatch to accept(String) " +
        "and convert raw bytes to String")
    void testJsonTextSerializer() {
        // given
        final String mockJson = "{\"op\":65636,\"data\":\"hello\"}";
        when(jsonSerializerMock.serialize(any())).thenReturn(mockJson);
        // when
        assertThat(textSerializer.getCodecDataType()).isEqualTo(DataType.TEXT);
        textSerializer.serializeAndAccept(TestOpCode.USER_DATA, "hello", visitorMock);
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
    @DisplayName("JsonBinaryEnvelopeSerializer should dispatch to accept(byte[]) " +
        "and pass raw bytes directly")
    void testJsonBinarySerializer() {
        // given
        final byte[] mockBytes = {1, 2, 3};
        when(jsonSerializerMock.serializeToBytes(any())).thenReturn(mockBytes);
        // when
        assertThat(binarySerializer.getCodecDataType()).isEqualTo(DataType.BINARY);
        binarySerializer.serializeAndAccept(TestOpCode.USER_DATA, "hello", visitorMock);
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
