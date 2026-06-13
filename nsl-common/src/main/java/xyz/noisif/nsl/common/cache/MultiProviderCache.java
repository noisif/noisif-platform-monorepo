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
package xyz.noisif.nsl.common.cache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

public class MultiProviderCache<K, C, V> {
  private final Map<K, List<V>> cache = new ConcurrentHashMap<>();
  private final List<V> providers;
  private final BiPredicate<V, C> supportsPredicate;

  public MultiProviderCache(List<V> providers, BiPredicate<V, C> supportsPredicate) {
    this.providers = providers;
    this.supportsPredicate = supportsPredicate;
  }

  public List<V> get(K key, C context) {
    if (key == null) {
      return List.of();
    }
    return cache.computeIfAbsent(
        key, k -> providers.stream().filter(p -> supportsPredicate.test(p, context)).toList());
  }
}
