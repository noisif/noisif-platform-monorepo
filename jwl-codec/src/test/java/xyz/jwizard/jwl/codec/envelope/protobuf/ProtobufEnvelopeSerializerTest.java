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
package xyz.jwizard.jwl.codec.envelope.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.ByteString;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.envelope.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.envelope.MessageEnvelope;
import xyz.jwizard.jwl.codec.envelope.TestOpCode;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;
import xyz.jwizard.jwl.codec.serialization.protobuf.ProtobufSerializer;
import xyz.jwizard.jwl.codec.serialization.protobuf.ProtobufSerializerException;
import xyz.jwizard.jwl.codec.serialization.protobuf.TestMessage;

@ExtendWith(MockitoExtension.class)
class ProtobufEnvelopeSerializerTest {
    @Mock
    private ProtobufSerializer protobufSerializerMock;
    @Mock
    private EncodedPayloadVisitor visitorMock;
    private ProtobufEnvelopeSerializer serializer;
    @Captor
    private ArgumentCaptor<byte[]> bytesCaptor;

    @BeforeEach
    void setUp() {
        serializer = ProtobufEnvelopeSerializer.createDefault(protobufSerializerMock);
    }

    @Test
    @DisplayName("should have proper base format and BINARY data type")
    void shouldHaveProperFormats() {
        assertThat(serializer.baseFormat()).isEqualTo(StandardSerializerFormat.PROTOBUF);
        assertThat(serializer.getCodecDataType()).isEqualTo(DataType.BINARY);
        assertThat(serializer.format().getFormat()).isEqualTo("protobuf+binary");
    }

    @Test
    @DisplayName("acceptRaw should immediately pass byte[] to binary visitor")
    void shouldPassRawBytesToVisitor() {
        // given
        final byte[] rawInput = {0x0F, 0x0A};
        // when
        serializer.acceptRaw(rawInput, visitorMock);
        // then
        verify(visitorMock).accept(rawInput);
    }

    @Test
    @DisplayName("should correctly serialize Protobuf message, wrap it in envelope " +
        "and dispatch as bytes")
    void shouldSerializeAndAccept() throws Exception {
        // given
        final TestMessage payload = TestMessage.newBuilder()
            .setId(101)
            .setValue("Wizard's Data")
            .build();
        final int expectedOp = TestOpCode.USER_DATA.getCode();
        // when
        serializer.serializeAndAccept(TestOpCode.USER_DATA, payload, visitorMock);
        // then
        verify(visitorMock).accept(bytesCaptor.capture());
        final byte[] outputBytes = bytesCaptor.getValue();
        final RawWsEnvelope envelope = RawWsEnvelope.parseFrom(outputBytes);
        assertThat(envelope.getOp()).isEqualTo(expectedOp);
        assertThat(envelope.getData()).isEqualTo(payload.toByteString());
    }

    @Test
    @DisplayName("should correctly unpack network bytes into MessageEnvelope with nested object")
    void shouldDeserializeEnvelope() {
        // given
        final int op = TestOpCode.USER_DATA.getCode();
        final TestMessage innerPayload = TestMessage.newBuilder()
            .setId(404)
            .setValue("Not Found")
            .build();
        final RawWsEnvelope networkEnvelope = RawWsEnvelope.newBuilder()
            .setOp(op)
            .setData(innerPayload.toByteString())
            .build();
        final byte[] networkBytes = networkEnvelope.toByteArray();

        when(protobufSerializerMock
            .deserializeFromBytes(any(byte[].class), eq(RawWsEnvelope.class))
        )
            .thenReturn(networkEnvelope);
        when(protobufSerializerMock.deserializeFromBytes(any(byte[].class), eq(TestMessage.class)))
            .thenReturn(innerPayload);
        final Function<Integer, Class<?>> typeResolver = code -> code == op
            ? TestMessage.class : null;
        // when
        final MessageEnvelope<?> result = serializer
            .deserializeEnvelope(networkBytes, typeResolver);
        // then
        assertThat(result.op()).isEqualTo(op);
        assertThat(result.data()).isInstanceOf(TestMessage.class);
        final TestMessage resultData = (TestMessage) result.data();
        assertThat(resultData.getId()).isEqualTo(404);
        assertThat(resultData.getValue()).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("should serialize null payload as EMPTY ByteString inside envelope")
    void shouldHandleNullPayload() throws Exception {
        // when
        serializer.serializeAndAccept(TestOpCode.USER_DATA, null, visitorMock);
        // then
        verify(visitorMock).accept(bytesCaptor.capture());
        final RawWsEnvelope envelope = RawWsEnvelope.parseFrom(bytesCaptor.getValue());
        assertThat(envelope.getOp()).isEqualTo(TestOpCode.USER_DATA.getCode());
        assertThat(envelope.getData()).isEqualTo(ByteString.EMPTY);
    }

    @Test
    @DisplayName("should fail fast if payload is not an instance of MessageLite")
    void shouldThrowWhenPayloadIsNotProtobuf() {
        // given
        final Object invalidPayload = "I am just a normal string, not a MessageLite";
        // then
        assertThatThrownBy(() -> serializer
            .serializeEnvelopeAsBytes(TestOpCode.USER_DATA, invalidPayload)
        )
            .isInstanceOf(ProtobufSerializerException.class)
            .hasMessageContaining("Payload must be a Protobuf MessageLite");
    }
}
