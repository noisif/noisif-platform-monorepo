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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

public class ProviderCache<K, C, V> {
  private final Map<K, V> cache = new ConcurrentHashMap<>();
  private final Set<V> providers;
  private final BiPredicate<V, C> supportsPredicate;

  public ProviderCache(Set<V> providers, BiPredicate<V, C> supportsPredicate) {
    this.providers = providers;
    this.supportsPredicate = supportsPredicate;
  }

  public V get(K key, C context) {
    if (key == null) {
      return null;
    }
    return cache.computeIfAbsent(key, k -> findFirstSupportedProvider(context));
  }

  private V findFirstSupportedProvider(C context) {
    return providers.stream()
        .filter(p -> supportsPredicate.test(p, context))
        .findFirst()
        .orElse(null);
  }
}
