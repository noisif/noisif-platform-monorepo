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
package xyz.jwizard.jwl.common.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GenericConcurrentRegistry<K, V>
    implements RegistryReader<K, V>, RegistryWriter<K, V> {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<K, V> registry = new ConcurrentHashMap<>();
    private final boolean allowOverwrite;

    protected GenericConcurrentRegistry() {
        this(false);
    }

    protected GenericConcurrentRegistry(boolean allowOverwrite) {
        this.allowOverwrite = allowOverwrite;
    }

    @Override
    public void register(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Registry key and value must not be null");
        }
        final V existing = registry.putIfAbsent(key, value);
        if (existing != null) {
            if (!allowOverwrite) {
                throw new IllegalStateException("Key '" + key +
                    "' is already registered in this registry");
            }
            log.warn("Overwriting existing value for key '{}', old value: [{}], new value: [{}]",
                key, existing, value);
            registry.put(key, value);
        } else {
            log.debug("Registered new value under key '{}'", key);
        }
    }

    @Override
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (log.isTraceEnabled()) {
            log.trace("Fetching value for key '{}'", key);
        }
        final V value = registry.get(key);
        if (value == null) {
            throw new IllegalArgumentException("No registered element found for key: " + key);
        }
        return value;
    }

    @Override
    public V getOrNull(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (log.isTraceEnabled()) {
            log.trace("Fetching value (or null) for key '{}'", key);
        }
        return registry.get(key);
    }

    protected V putDirect(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Registry key and value must not be null");
        }
        if (log.isTraceEnabled()) {
            log.trace("Directly putting value for key '{}'", key);
        }
        return registry.put(key, value);
    }

    @Override
    public boolean remove(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        final V removed = registry.remove(key);
        if (removed != null) {
            log.debug("Removed element under key '{}'", key);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeDirect(K key, V value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Registry key and value must not be null");
        }
        final boolean removed = registry.remove(key, value);
        if (removed) {
            log.debug("Atomically removed specific value under key '{}'", key);
        }
        return removed;
    }

    @Override
    public void clear() {
        final int size = registry.size();
        registry.clear();
        log.debug("Cleared registry (removed {} elements)", size);
    }

    @Override
    public Collection<V> getAll() {
        if (log.isTraceEnabled()) {
            log.trace("Retrieving all registered values (total: {})", registry.size());
        }
        return Collections.unmodifiableCollection(registry.values());
    }

    @Override
    public Map<K, V> getEntries() {
        if (log.isTraceEnabled()) {
            log.trace("Retrieving all registry entries (total: {})", registry.size());
        }
        return Collections.unmodifiableMap(registry);
    }
}
