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
package xyz.jwizard.jwl.common.util.math;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MathUtilTest {
  @Test
  @DisplayName("should return zero if base delay is zero or negative")
  void shouldReturnZeroForInvalidBaseDelay() {
    // given
    final long zeroBase = 0;
    final long negativeBase = -50;
    // when
    final long resultZero = MathUtil.calcExpBackoff(1, zeroBase, 1000);
    final long resultNegative = MathUtil.calcExpBackoff(1, negativeBase, 1000);
    // then
    assertThat(resultZero).isZero();
    assertThat(resultNegative).isZero();
  }

  @ParameterizedTest
  @CsvSource({
    "1, 100, 90, 110", // 100 * 2^0 = 100 (+/- 10%)
    "2, 100, 180, 220", // 100 * 2^1 = 200 (+/- 10%)
    "3, 100, 360, 440", // 100 * 2^2 = 400 (+/- 10%)
    "4, 100, 720, 880" // 100 * 2^3 = 800 (+/- 10%)
  })
  @DisplayName("should calculate exponential delay within jitter range")
  void shouldCalculateExpDelayWithJitter(int attempt, long baseDelay, long min, long max) {
    // given: csv
    // when
    final long delay = MathUtil.calcExpBackoff(attempt, baseDelay, 10000);
    // then
    assertThat(delay).isBetween(min, max);
  }

  @Test
  @DisplayName("should cap the delay at maxDelayMs")
  void shouldCapMaxDelay() {
    // given
    final int highAttempt = 20;
    final long baseDelay = 100;
    final long maxLimit = 1000;
    // when
    final long delay = MathUtil.calcExpBackoff(highAttempt, baseDelay, maxLimit);
    // then
    assertThat(delay).isEqualTo(maxLimit);
  }

  @Test
  @DisplayName("should treat negative or zero attempt as attempt 1")
  void shouldHandleInvalidAttempts() {
    // given
    final long baseDelay = 100;
    final long maxDelay = 1000;
    // when
    final long delayZero = MathUtil.calcExpBackoff(0, baseDelay, maxDelay);
    final long delayNegative = MathUtil.calcExpBackoff(-5, baseDelay, maxDelay);
    // then
    assertThat(delayZero).isBetween(90L, 110L);
    assertThat(delayNegative).isBetween(90L, 110L);
  }
}
