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
package xyz.noisif.nsl.common.limit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

class TokenBucketRateLimiterTest {
  private static final long CAPACITY = 5;
  private static final long REFILL_TOKENS = 5;
  private static final Duration REFILL_PERIOD = Duration.ofMillis(200);

  private RateLimiter rateLimiter;

  @BeforeEach
  void setUp() {
    rateLimiter =
        TokenBucketRateLimiter.builder()
            .capacity(CAPACITY)
            .refillTokens(REFILL_TOKENS)
            .refillPeriod(REFILL_PERIOD)
            .build();
  }

  @Test
  @DisplayName("should allow requests up to configured capacity")
  void shouldAllowRequestsUpToCapacity() {
    // given
    final String key = "user-1";
    // when & then
    for (int i = 0; i < CAPACITY; i++) {
      assertTrue(rateLimiter.tryAcquire(key), "Should acquire token for request " + (i + 1));
    }
  }

  @Test
  @DisplayName("should reject requests when capacity is exceeded")
  void shouldRejectRequestsWhenCapacityExceeded() {
    // given
    final String key = "user-2";
    // when
    for (int i = 0; i < CAPACITY; i++) {
      rateLimiter.tryAcquire(key);
    }
    // then
    assertFalse(rateLimiter.tryAcquire(key), "Should reject request after exceeding capacity");
  }

  @Test
  @DisplayName("should isolate requests from different keys")
  void shouldIsolateDifferentKeys() {
    // given
    final String userA = "user-A";
    final String userB = "user-B";
    // when
    for (int i = 0; i < CAPACITY; i++) {
      rateLimiter.tryAcquire(userA);
    }
    // then
    assertFalse(rateLimiter.tryAcquire(userA), "User A should be rate limited");
    assertTrue(rateLimiter.tryAcquire(userB), "User B should not be affected by User A's limit");
  }

  @Test
  @DisplayName("should reset bucket capacity for a given key")
  void shouldResetBucketForKey() {
    // given
    final String key = "user-3";
    for (int i = 0; i < CAPACITY; i++) {
      rateLimiter.tryAcquire(key);
    }
    assertFalse(rateLimiter.tryAcquire(key));
    // when
    rateLimiter.reset(key);
    // then
    assertTrue(rateLimiter.tryAcquire(key), "Should acquire token successfully after bucket reset");
  }

  @Test
  @DisplayName("should refill tokens over time")
  void shouldRefillTokensOverTime() throws InterruptedException {
    // given
    final String key = "user-4";
    for (int i = 0; i < CAPACITY; i++) {
      rateLimiter.tryAcquire(key);
    }
    assertFalse(rateLimiter.tryAcquire(key));
    // when
    Thread.sleep(REFILL_PERIOD.toMillis() + 50);
    // then
    assertTrue(rateLimiter.tryAcquire(key), "Should acquire token after refill period has passed");
  }
}
