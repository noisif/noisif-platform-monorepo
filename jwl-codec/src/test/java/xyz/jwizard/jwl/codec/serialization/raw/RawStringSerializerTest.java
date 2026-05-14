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
package xyz.jwizard.jwl.codec.serialization.raw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.serialization.MessageSerializerException;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;

class RawStringSerializerTest {
    private final RawStringSerializer serializer = RawStringSerializer.createDefault();

    @Test
    @DisplayName("should return the same string on serialization")
    void shouldSerializeRawString() {
        // given
        final String input = "hello jwizard text";
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
        assertThat(serializer.format()).isEqualTo(StandardSerializerFormat.RAW);
    }

    @Test
    @DisplayName("should return TEXT data type")
    void shouldReturnTextDataType() {
        assertThat(serializer.getCodecDataType()).isEqualTo(DataType.TEXT);
    }
}
