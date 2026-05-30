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
package xyz.jwizard.jwl.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class ProviderCacheTest {
  private ProviderCache<String, String, String> providerCache;
  private AtomicInteger callCount;

  @BeforeEach
  void setUp() {
    callCount = new AtomicInteger(0);
    // providers are defined as simple strings for testing
    final Set<String> providers = Set.of("AlphaProvider", "BetaProvider");
    providerCache =
        new ProviderCache<>(
            providers,
            (provider, context) -> {
              // increment counter to track how many times the predicate is actually
              // executed
              callCount.incrementAndGet();
              return provider.startsWith(context);
            });
  }

  @Test
  @DisplayName("should find and cache correct provider")
  void shouldFindAndCacheProvider() {
    // when: first call for a specific key
    final String provider = providerCache.get("user-key", "Alpha");
    // Then
    assertThat(provider).isEqualTo("AlphaProvider");
    final int initialCalls = callCount.get();
    assertThat(initialCalls).isPositive();
    // when: second call for the same key
    final String cachedProvider = providerCache.get("user-key", "Alpha");
    // then: result is the same but predicate execution count does not increase
    assertThat(cachedProvider).isEqualTo("AlphaProvider");
    assertThat(callCount.get()).isEqualTo(initialCalls);
  }

  @Test
  @DisplayName("should return null if no provider supports context")
  void shouldReturnNullIfNoMatch() {
    // when: looking for a non-existent provider
    final String provider = providerCache.get("unknown-key", "Gamma");
    // then: return null as expected
    assertThat(provider).isNull();
  }

  @Test
  @DisplayName("should return null for null key")
  void shouldReturnNullForNullKey() {
    // when: key is null
    final String provider = providerCache.get(null, "Alpha");
    // then: return null immediately without checking providers
    assertThat(provider).isNull();
    assertThat(callCount.get()).isZero();
  }

  @Test
  @DisplayName("should handle multiple keys independently")
  void shouldCacheMultipleKeys() {
    // when: request two different keys
    final String p1 = providerCache.get("key1", "Alpha");
    final String p2 = providerCache.get("key2", "Beta");
    // then: both are resolved
    assertThat(p1).isEqualTo("AlphaProvider");
    assertThat(p2).isEqualTo("BetaProvider");
    final int callsAfterFirstTwo = callCount.get();
    // when: call again for the same keys
    providerCache.get("key1", "Alpha");
    providerCache.get("key2", "Beta");
    // then: call count remains the same due to caching
    assertThat(callCount.get()).isEqualTo(callsAfterFirstTwo);
  }
}
