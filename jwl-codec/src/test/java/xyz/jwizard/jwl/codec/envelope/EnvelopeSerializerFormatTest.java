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
package xyz.jwizard.jwl.codec.envelope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.serialization.SerializerFormat;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;

class EnvelopeSerializerFormatTest {
    @Test
    @DisplayName("should properly combine base format and data type into full format string")
    void shouldCombineFormatAndDataType() {
        // given
        final SerializerFormat base = StandardSerializerFormat.JSON;
        final DataType type = DataType.TEXT;
        // when
        final EnvelopeSerializerFormat format = EnvelopeSerializerFormat.from(base, type);
        // then
        assertThat(format.getFormat()).isEqualTo("json+text");
        assertThat(format.toString()).isEqualTo("json+text");
        assertThat(format.baseFormat()).isEqualTo(base);
        assertThat(format.dataType()).isEqualTo(type);
    }

    @Test
    @DisplayName("should throw exception on unsupported default text methods")
    void shouldThrowOnDefaultInterfaceMethods() {
        // given
        EnvelopeSerializer<byte[]> defaultSerializer = new EnvelopeSerializer<>() {
            @Override
            public SerializerFormat baseFormat() {
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
            public void serializeAndAccept(OpCode opCode, Object payload,
                                           EncodedPayloadVisitor visitor) {
            }

            @Override
            public void acceptRaw(byte[] rawPayload, EncodedPayloadVisitor visitor) {
            }

            @Override
            public MessageEnvelope<?> deserializeEnvelope(byte[] payload,
                                                          Function<Integer, Class<?>> typeResolver) {
                return null;
            }
        };
        // then
        assertThatThrownBy(() -> defaultSerializer
            .serializeEnvelopeAsString(TestOpCode.USER_DATA, "test")
        )
            .isInstanceOf(UnsupportedEnvelopeDataTypeException.class)
            .hasMessageContaining("Text frames are not supported by protobuf+binary");
        assertThatThrownBy(() -> defaultSerializer.deserializeEnvelope("{}", id -> String.class))
            .isInstanceOf(UnsupportedEnvelopeDataTypeException.class)
            .hasMessageContaining("Text frames are not supported by protobuf+binary");
    }
}
