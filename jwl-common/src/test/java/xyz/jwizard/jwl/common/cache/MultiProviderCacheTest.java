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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MultiProviderCacheTest {
    private MultiProviderCache<String, String, String> providerCache;
    private AtomicInteger callCount;

    @BeforeEach
    void setUp() {
        callCount = new AtomicInteger(0);
        final List<String> providers = List.of(
            "AlphaProvider1",
            "BetaProvider",
            "AlphaProvider2"
        );
        providerCache = new MultiProviderCache<>(providers, (provider, context) -> {
            // increment counter to track how many times the predicate is actually executed
            callCount.incrementAndGet();
            return provider.startsWith(context);
        });
    }

    @Test
    @DisplayName("should find, cache and return all matching providers in order")
    void shouldFindAndCacheMultipleProviders() {
        // when
        final List<String> matchedProviders = providerCache.get("user-key", "Alpha");
        // then
        assertThat(matchedProviders).containsExactly("AlphaProvider1", "AlphaProvider2");
        final int initialCalls = callCount.get();
        assertThat(initialCalls).isPositive();
        // when
        final List<String> cachedProviders = providerCache.get("user-key", "Alpha");
        // then
        assertThat(cachedProviders).containsExactly("AlphaProvider1", "AlphaProvider2");
        assertThat(callCount.get()).isEqualTo(initialCalls);
    }

    @Test
    @DisplayName("should return an empty list if no provider supports context")
    void shouldReturnEmptyListIfNoMatch() {
        // when
        final List<String> providers = providerCache.get("unknown-key", "Gamma");
        // then
        assertThat(providers).isEmpty();
        assertThat(providers).isNotNull();
    }

    @Test
    @DisplayName("should return an empty list for null key")
    void shouldReturnEmptyListForNullKey() {
        // when
        final List<String> providers = providerCache.get(null, "Alpha");
        // thens
        assertThat(providers).isEmpty();
        assertThat(callCount.get()).isZero();
    }

    @Test
    @DisplayName("should handle multiple keys independently and cache them")
    void shouldCacheMultipleKeys() {
        // when
        final List<String> p1 = providerCache.get("key1", "Alpha");
        final List<String> p2 = providerCache.get("key2", "Beta");
        // then
        assertThat(p1).containsExactly("AlphaProvider1", "AlphaProvider2");
        assertThat(p2).containsExactly("BetaProvider");
        final int callsAfterFirstTwo = callCount.get();
        // when
        providerCache.get("key1", "Alpha");
        providerCache.get("key2", "Beta");
        // then
        assertThat(callCount.get()).isEqualTo(callsAfterFirstTwo);
    }
}
