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
package xyz.jwizard.jwl.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CodecUtilTest {
    @Test
    @DisplayName("should encode string to Base64 format")
    void shouldEncodeBase64() {
        // given
        final String rawText = "jwl:secretPassword123!";
        final String expectedBase64 = "andsOnNlY3JldFBhc3N3b3JkMTIzIQ==";
        // when
        final String encoded = CodecUtil.encodeBase64(rawText);
        // then
        assertThat(encoded).isEqualTo(expectedBase64);
    }

    @Test
    @DisplayName("should decode Base64 string to raw text")
    void shouldDecodeBase64() {
        // given
        final String encodedBase64 = "andsOnNlY3JldFBhc3N3b3JkMTIzIQ==";
        final String expectedText = "jwl:secretPassword123!";
        // when
        final String decoded = CodecUtil.decodeBase64(encodedBase64);
        // then
        assertThat(decoded).isEqualTo(expectedText);
    }

    @Test
    @DisplayName("should encode and decode special UTF-8 characters correctly")
    void shouldHandleUtf8Characters() {
        // given
        final String rawText = "Zażółć gęślą jaźń";
        // when
        final String encoded = CodecUtil.encodeBase64(rawText);
        final String decoded = CodecUtil.decodeBase64(encoded);
        // then
        assertThat(encoded).isNotEqualTo(rawText);
        assertThat(decoded).isEqualTo(rawText);
    }

    @Test
    @DisplayName("should return null when input is null")
    void shouldReturnNullForNullInput() {
        // when & then
        assertThat(CodecUtil.encodeBase64(null)).isNull();
        assertThat(xyz.jwizard.jwl.common.util.CodecUtil.decodeBase64(null)).isNull();
    }

    @Test
    @DisplayName("should throw exception when decoding invalid Base64 string")
    void shouldThrowExceptionForInvalidBase64() {
        // given
        final String invalidBase64 = "This is not valid base64!@#";
        // when & then
        assertThatThrownBy(() -> xyz.jwizard.jwl.common.util.CodecUtil.decodeBase64(invalidBase64))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
