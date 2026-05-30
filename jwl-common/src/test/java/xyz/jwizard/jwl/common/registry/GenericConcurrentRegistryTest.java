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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class GenericConcurrentRegistryTest {
  @Test
  @DisplayName("should throw IllegalArgumentException on null parameters")
  void shouldThrowOnNulls() {
    // given
    final StrictRegistry registry = new StrictRegistry();
    // when & then
    assertThrows(IllegalArgumentException.class, () -> registry.register(null, "value"));
    assertThrows(IllegalArgumentException.class, () -> registry.register("key", null));
    assertThrows(IllegalArgumentException.class, () -> registry.get(null));
    assertThrows(IllegalArgumentException.class, () -> registry.getOrNull(null));
    assertThrows(IllegalArgumentException.class, () -> registry.remove(null));
    assertThrows(IllegalArgumentException.class, () -> registry.removeDirect(null, "val"));
  }

  @Test
  @DisplayName("should throw IllegalStateException on duplicate key if overwrite is disabled")
  void shouldThrowOnDuplicateKeyWhenStrict() {
    // given
    final StrictRegistry registry = new StrictRegistry();
    registry.register("key1", "val1");
    // when
    final IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> registry.register("key1", "val2"));
    // then
    assertTrue(ex.getMessage().contains("is already registered"));
    assertEquals("val1", registry.get("key1"));
  }

  @Test
  @DisplayName("should overwrite value if overwrite is enabled")
  void should_overwrite_when_lenient() {
    // given
    final LenientRegistry registry = new LenientRegistry();
    registry.register("key1", "val1");
    // when
    registry.register("key1", "val2");
    // then
    assertEquals("val2", registry.get("key1"));
  }

  @Test
  @DisplayName("concurrency: should safely register 10,000 unique keys from 100 threads")
  void should_register_concurrently_without_data_loss() throws InterruptedException {
    // given
    final StrictRegistry registry = new StrictRegistry();
    final int numThreads = 100;
    final int keysPerThread = 100;
    final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    final CountDownLatch startGun = new CountDownLatch(1);
    final CountDownLatch finishLine = new CountDownLatch(numThreads);
    // when
    for (int i = 0; i < numThreads; i++) {
      final int threadId = i;
      executor.execute(
          () -> {
            try {
              startGun.await();
              for (int j = 0; j < keysPerThread; j++) {
                registry.register("key-" + threadId + "-" + j, "value");
              }
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            } finally {
              finishLine.countDown();
            }
          });
    }
    startGun.countDown();
    assertTrue(finishLine.await(5, TimeUnit.SECONDS), "Threads timed out");
    executor.shutdown();
    executor.close();
    // then
    assertEquals(numThreads * keysPerThread, registry.getAll().size());
    assertEquals(numThreads * keysPerThread, registry.getEntries().size());
  }

  @Test
  @DisplayName(
      "concurrency: only ONE thread should succeed when registering the same key "
          + "(Strict Mode)")
  void should_prevent_race_condition_on_duplicate_keys() throws InterruptedException {
    // given
    final StrictRegistry registry = new StrictRegistry();
    final int numThreads = 100;
    final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    final CountDownLatch startGun = new CountDownLatch(1);
    final CountDownLatch finishLine = new CountDownLatch(numThreads);
    final AtomicInteger successes = new AtomicInteger(0);
    final AtomicInteger failures = new AtomicInteger(0);
    // when
    for (int i = 0; i < numThreads; i++) {
      final int threadId = i;
      executor.execute(
          () -> {
            try {
              startGun.await();
              registry.register("RACE_KEY", "VALUE_" + threadId);
              successes.incrementAndGet();
            } catch (IllegalStateException ex) {
              failures.incrementAndGet();
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            } finally {
              finishLine.countDown();
            }
          });
    }
    startGun.countDown();
    assertTrue(finishLine.await(5, TimeUnit.SECONDS));
    executor.shutdown();
    executor.close();
    // then
    assertEquals(1, successes.get(), "Exactly one thread should have registered the key");
    assertEquals(numThreads - 1, failures.get(), "All other threads should have thrown exception");
    assertEquals(1, registry.getAll().size());
  }

  @Test
  @DisplayName("concurrency: removeDirect should return true for exactly ONE thread")
  void should_atomically_remove_direct() throws InterruptedException {
    // given
    final StrictRegistry registry = new StrictRegistry();
    registry.register("TARGET_KEY", "TARGET_VALUE");
    final int numThreads = 100;
    final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    final CountDownLatch startGun = new CountDownLatch(1);
    final CountDownLatch finishLine = new CountDownLatch(numThreads);
    final AtomicInteger successfulRemoves = new AtomicInteger(0);
    // when
    for (int i = 0; i < numThreads; i++) {
      executor.execute(
          () -> {
            try {
              startGun.await();
              if (registry.removeDirect("TARGET_KEY", "TARGET_VALUE")) {
                successfulRemoves.incrementAndGet();
              }
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            } finally {
              finishLine.countDown();
            }
          });
    }
    startGun.countDown();
    assertTrue(finishLine.await(5, TimeUnit.SECONDS));
    executor.shutdown();
    executor.close();
    // then
    assertEquals(
        1, successfulRemoves.get(), "Only one thread should have successfully removed the value");
    assertEquals(0, registry.getAll().size(), "Registry should be empty");
  }
}

class StrictRegistry extends GenericConcurrentRegistry<String, String> {
  StrictRegistry() {
    super(false);
  }
}

class LenientRegistry extends GenericConcurrentRegistry<String, String> {
  LenientRegistry() {
    super(true);
  }
}
