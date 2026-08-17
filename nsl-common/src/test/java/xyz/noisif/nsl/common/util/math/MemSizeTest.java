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
package xyz.noisif.nsl.common.util.math;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MemSizeTest {
  @ParameterizedTest
  @CsvSource({
    "1024, 1024", // raw bytes
    "0, 0", // zero
    "1K, 1024", // kilobytes
    "2k, 2048", // lower case support
    "1M, 1048576", // megabytes
    "5m, 5242880",
    "1G, 1073741824", // gigabytes
    "2g, 2147483648",
    "'  10M  ', 10485760" // whitespaces handling
  })
  @DisplayName("should parse valid memory strings to exact bytes")
  void shouldParseValidMemoryStrings(String input, long expectedBytes) {
    // given: csv
    // when
    final long result = MemSize.parseFromStr(input);
    // then
    assertEquals(expectedBytes, result);
  }

  @Test
  @DisplayName("should return zero for null, empty or blank inputs")
  void shouldReturnZeroForNullOrBlank() {
    // given & when
    final long resultNull = MemSize.parseFromStr(null);
    final long resultEmpty = MemSize.parseFromStr("");
    final long resultBlank = MemSize.parseFromStr("   ");
    // then
    assertThat(resultNull).isZero();
    assertThat(resultEmpty).isZero();
    assertThat(resultBlank).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"10Z", "100X", "5TB"})
  @DisplayName("should throw exception for unsupported memory unit suffix")
  void shouldThrowOnUnsupportedSuffix(String invalidInput) {
    // given: value source
    // when & then
    assertThrows(IllegalArgumentException.class, () -> MemSize.parseFromStr(invalidInput));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "abcM", "10M20M"})
  @DisplayName("should throw exception for malformed numeric part")
  void shouldThrowOnMalformedNumber(String badNumberInput) {
    // given: value source
    // when & then
    assertThrows(NumberFormatException.class, () -> MemSize.parseFromStr(badNumberInput));
  }
}
