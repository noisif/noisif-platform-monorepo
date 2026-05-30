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
package xyz.jwizard.jwl.kv.jedis.pubsub.pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegexChannelParamExtractorTest {
  @Test
  @DisplayName("should return empty array when pattern has no wildcards")
  void shouldHandleExactMatch() {
    // given
    final RegexChannelParamExtractor extractor =
        new RegexChannelParamExtractor("sys:global:events");
    // when
    final String[] params = extractor.extract("sys:global:events");
    // then
    assertEquals(0, params.length);
  }

  @Test
  @DisplayName("should extract single parameter from wildcard")
  void shouldExtractSingleWildcard() {
    // given
    final RegexChannelParamExtractor extractor =
        new RegexChannelParamExtractor("user:*:notifications");
    // when
    final String[] params = extractor.extract("user:jwizard_123:notifications");
    // then
    assertEquals(1, params.length);
    assertEquals("jwizard_123", params[0]);
  }

  @Test
  @DisplayName("should extract multiple parameters from multiple wildcards")
  void shouldExtractMultipleWildcards() {
    // given
    final RegexChannelParamExtractor extractor =
        new RegexChannelParamExtractor("game:*:match:*:player:*:stats");
    // when
    final String[] params = extractor.extract("game:lol:match:999:player:faker:stats");
    // then
    assertEquals(3, params.length);
    assertArrayEquals(new String[] {"lol", "999", "faker"}, params);
  }

  @Test
  @DisplayName("should return empty array when channel does not match pattern")
  void shouldReturnEmptyWhenNoMatch() {
    // given
    final RegexChannelParamExtractor extractor =
        new RegexChannelParamExtractor("user:*:notifications");
    // when
    final String[] params = extractor.extract("user:123:other_events");
    // then
    assertEquals(0, params.length);
  }

  @Test
  @DisplayName("should handle nulls gracefully")
  void shouldHandleNulls() {
    // given
    final RegexChannelParamExtractor extractor = new RegexChannelParamExtractor(null);
    // when & then
    assertEquals(0, extractor.extract("some:channel").length);
    assertEquals(0, extractor.extract(null).length);
  }
}
