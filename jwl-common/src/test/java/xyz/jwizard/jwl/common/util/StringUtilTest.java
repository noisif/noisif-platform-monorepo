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
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StringUtilTest {
  @Test
  @DisplayName("should handle basic string truncation (null, empty, ASCII, shorter than max)")
  void shouldHandleBasicTruncation() {
    assertAll(
        () -> assertThat(StringUtil.truncateToUtf8Bytes(null, 10)).isNull(),
        () -> assertThat(StringUtil.truncateToUtf8Bytes("", 10)).isEmpty(),
        () -> assertThat(StringUtil.truncateToUtf8Bytes("Hello", 10)).isEqualTo("Hello"),
        () -> assertThat(StringUtil.truncateToUtf8Bytes("Hello World", 5)).isEqualTo("Hello"));
  }

  @Test
  @DisplayName("should safely truncate complex multi-byte UTF-8 characters without corruption")
  void shouldSafelyTruncateMultibyteCharacters() {
    assertAll(
        () -> assertThat(StringUtil.truncateToUtf8Bytes("aą", 2)).isEqualTo("a"),
        () -> assertThat(StringUtil.truncateToUtf8Bytes("aą", 3)).isEqualTo("aą"),
        () -> assertThat(StringUtil.truncateToUtf8Bytes("Hi 🚀", 5)).isEqualTo("Hi "),
        () -> assertThat(StringUtil.truncateToUtf8Bytes("Hi 🚀!", 7)).isEqualTo("Hi 🚀"));
  }
}
