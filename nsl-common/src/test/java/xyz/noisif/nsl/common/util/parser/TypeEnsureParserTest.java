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
package xyz.noisif.nsl.common.util.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

class TypeEnsureParserTest {
  @Test
  @DisplayName("should correctly parse and cast valid raw value")
  void shouldParseAndCastValidValue() {
    // given
    final String rawValue = "1024";
    final Function<String, Object> transformer = Long::valueOf;
    final Long defaultValue = 999L;
    // when
    final Long result =
        TypeEnsureParser.parseAndCast(rawValue, transformer, Long.class, defaultValue);
    // then
    assertThat(result).isEqualTo(1024L);
  }

  @Test
  @DisplayName("should return default value when raw value is null")
  void shouldReturnDefaultWhenRawValueIsNull() {
    // given
    final Function<String, Object> transformer = Long::valueOf;
    final Long defaultValue = 999L;
    // when
    final Long result = TypeEnsureParser.parseAndCast(null, transformer, Long.class, defaultValue);
    // then
    assertThat(result).isEqualTo(defaultValue);
  }

  @Test
  @DisplayName("should return default value when transformer throws exception")
  void shouldReturnDefaultWhenTransformerThrowsException() {
    // given
    final String rawValue = "not-a-number";
    final Function<String, Object> transformer =
        val -> {
          throw new NumberFormatException();
        };
    final Long defaultValue = 999L;
    // when
    final Long result =
        TypeEnsureParser.parseAndCast(rawValue, transformer, Long.class, defaultValue);
    // then
    assertThat(result).isEqualTo(defaultValue);
  }

  @Test
  @DisplayName("should return default value when transformer returns null")
  void shouldReturnDefaultWhenTransformerReturnsNull() {
    // given
    final String rawValue = "1024";
    final Function<String, Object> transformer = val -> null;
    final Long defaultValue = 999L;
    // when
    final Long result =
        TypeEnsureParser.parseAndCast(rawValue, transformer, Long.class, defaultValue);
    // then
    assertThat(result).isEqualTo(defaultValue);
  }

  @Test
  @DisplayName("should return default value when transformer returns incorrect type")
  void shouldReturnDefaultWhenTransformerReturnsIncorrectType() {
    // given
    final String rawValue = "1024";
    final Function<String, Object> transformer = val -> "Im a string, not a long";
    final Long defaultValue = 999L;
    // when
    final Long result =
        TypeEnsureParser.parseAndCast(rawValue, transformer, Long.class, defaultValue);
    // then
    assertThat(result).isEqualTo(defaultValue);
  }
}
