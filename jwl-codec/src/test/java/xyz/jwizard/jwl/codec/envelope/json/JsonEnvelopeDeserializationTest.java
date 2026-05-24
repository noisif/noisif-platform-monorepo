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
package xyz.jwizard.jwl.codec.envelope.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import xyz.jwizard.jwl.codec.envelope.MessageEnvelope;
import xyz.jwizard.jwl.codec.envelope.TestOpCode;
import xyz.jwizard.jwl.codec.serialization.json.JsonSerializer;
import xyz.jwizard.jwl.codec.serialization.json.JsonSerializerException;

@ExtendWith(MockitoExtension.class)
class JsonEnvelopeDeserializationTest {
    @Mock
    private JsonSerializer jsonSerializerMock;
    private JsonTextEnvelopeSerializer serializer;
    private Function<Integer, Class<?>> typeResolver;

    @BeforeEach
    void setUp() {
        serializer = JsonTextEnvelopeSerializer.createDefault(jsonSerializerMock);
        typeResolver = op -> op == TestOpCode.USER_DATA.getCode() ? String.class : null;
    }

    @Test
    @DisplayName("should properly parse map into MessageEnvelope")
    void shouldParseValidEnvelope() {
        // given
        final Map<String, Object> mockTree = Map.of(
            "op", TestOpCode.USER_DATA.getCode(),
            "data", "test_payload"
        );
        when(jsonSerializerMock.deserialize(any(String.class), eq(Map.class))).thenReturn(mockTree);
        when(jsonSerializerMock.convert("test_payload", String.class)).thenReturn("test_payload");
        // when
        final MessageEnvelope<?> envelope = serializer.unwrap("{}", typeResolver);
        // then
        assertThat(envelope.op()).isEqualTo(TestOpCode.USER_DATA.getCode());
        assertThat(envelope.data()).isEqualTo("test_payload");
    }

    @Test
    @DisplayName("should throw exception when 'op' field is missing")
    void shouldThrowWhenOpIsMissing() {
        // given
        final Map<String, Object> mockTree = Map.of("data", "no_op_here");
        when(jsonSerializerMock.deserialize(any(String.class), eq(Map.class))).thenReturn(mockTree);
        // then
        assertThatThrownBy(() -> serializer.unwrap("{}", typeResolver))
            .isInstanceOf(JsonSerializerException.class)
            .hasMessageContaining("Missing or invalid 'op' field");
    }

    @Test
    @DisplayName("should throw exception when OP code is unknown to resolver")
    void shouldThrowOnUnknownOpCode() {
        // given
        final int nonExistingOpCode = 999999;
        final Map<String, Object> mockTree = Map.of("op", nonExistingOpCode);
        when(jsonSerializerMock.deserialize(any(String.class), eq(Map.class))).thenReturn(mockTree);
        // when
        final MessageEnvelope<?> envelope = serializer.unwrap("{}", typeResolver);
        // then
        assertThat(envelope).isNotNull();
        assertThat(envelope.op()).isEqualTo(nonExistingOpCode);
        assertThat(envelope.data()).isNull();
    }
}
