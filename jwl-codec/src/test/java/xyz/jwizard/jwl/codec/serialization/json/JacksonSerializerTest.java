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
package xyz.jwizard.jwl.codec.serialization.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.codec.serialization.SerializerFormat;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;

class JacksonSerializerTest {
    private final JacksonSerializer strict = JacksonSerializer.createDefaultStrictMapper();
    private final JacksonSerializer lenient = JacksonSerializer.createLenientForMessaging();

    @Test
    @DisplayName("should serialize and deserialize using strings")
    void shouldHandleStrings() {
        // given
        final Simple person = new Simple("JWizard");
        // when
        final String json = strict.serialize(person);
        final Simple result = strict.deserialize(json, Simple.class);
        // then
        assertThat(json).contains("\"name\":\"JWizard\"");
        assertThat(result).isEqualTo(person);
    }

    @Test
    @DisplayName("should serialize and deserialize using byte arrays")
    void shouldHandleBytes() {
        // given
        final Simple person = new Simple("ByteWizard");
        // when
        final byte[] bytes = strict.serializeToBytes(person);
        final Simple result = strict.deserializeFromBytes(bytes, Simple.class);
        // then
        assertThat(bytes).isNotEmpty();
        assertThat(result).isEqualTo(person);
    }

    @Test
    @DisplayName("should serialize and deserialize using streams")
    void shouldHandleStreams() {
        // given
        final Simple person = new Simple("StreamWizard");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        // when
        strict.serializeToStream(person, out);
        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final Simple result = strict.deserializeFromStream(in, Simple.class);
        // then
        assertThat(result).isEqualTo(person);
    }

    @Test
    @DisplayName("should convert map to object type")
    void shouldConvertTypes() {
        // given
        final Map<String, Object> map = Map.of("name", "ConvertedWizard");
        // when
        final Simple result = strict.convert(map, Simple.class);
        // then
        assertThat(result.name()).isEqualTo("ConvertedWizard");
    }

    @Test
    @DisplayName("strict mapper should throw exception on unknown json properties")
    void strictFailsOnUnknown() {
        // given
        final String json = "{\"name\":\"J\", \"unknown_field\": true}";
        // when & then
        assertThatThrownBy(() -> strict.deserialize(json, Simple.class))
            .isInstanceOf(JsonSerializerException.class);
    }

    @Test
    @DisplayName("lenient mapper should ignore unknown json properties")
    void lenientIgnoresUnknown() {
        // given
        final String json = "{\"name\":\"J\", \"unknown_field\": true}";
        // when
        final Simple result = lenient.deserialize(json, Simple.class);
        // then
        assertThat(result.name()).isEqualTo("J");
    }

    @Test
    @DisplayName("should throw exception when required record property is missing")
    void failsOnMissingProperties() {
        // given
        final String json = "{}";
        // when & then
        assertThatThrownBy(() -> lenient.deserialize(json, Simple.class))
            .isInstanceOf(JsonSerializerException.class);
    }

    @Test
    @DisplayName("should throw exception when null is assigned to primitive type")
    void failsOnNullPrimitives() {
        // given
        final String json = "{\"id\": null}";
        // when & then
        assertThatThrownBy(() -> strict.deserialize(json, PrimitiveTest.class))
            .isInstanceOf(JsonSerializerException.class);
    }

    @Test
    @DisplayName("should return correct serializer format")
    void shouldReturnCorrectFormat() {
        // when
        final SerializerFormat format = strict.getFormat();
        // then
        assertThat(format).isEqualTo(StandardSerializerFormat.JSON);
    }

    @Test
    @DisplayName("should provide clean error message on malformed JSON")
    void shouldThrowCleanException() {
        // given
        final String badJson = "{ \"name\": missing_quotes }";
        // when & then
        assertThatThrownBy(() -> strict.deserialize(badJson, Simple.class))
            .isInstanceOf(JsonSerializerException.class)
            .hasMessageNotContaining("at [Source");
    }
}

record Simple(String name) {
}

record PrimitiveTest(int id) {
}
