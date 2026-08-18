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
package xyz.noisif.nsl.zcache.lru;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.noisif.nsl.common.util.math.MemUnit;
import xyz.noisif.nsl.zcache.GenericNativeStorage;
import xyz.noisif.nsl.zcache.NativeStoragePayload;
import xyz.noisif.nsl.zcache.TestKey;

class ConcurrentLruNativeStorageTest {
  private GenericNativeStorage storage;

  @BeforeEach
  void setUp() {
    storage =
        ConcurrentLruNativeStorage.builder()
            .maxMemory(10, MemUnit.BYTES)
            .initialCapacity(16)
            .build();
    storage.start();
  }

  @AfterEach
  void tearDown() {
    storage.close();
  }

  @Test
  @DisplayName("should store and fetch payload, incrementing reference count")
  void shouldStoreAndFetch() {
    // given
    final TestKey key = new TestKey("my_data");
    final byte[] data = new byte[] {1, 2, 3, 4}; // 4 bytes
    // when
    storage.store(key, data);
    final NativeStoragePayload fetched = storage.fetch(key);
    // then
    assertThat(fetched).isNotNull();
    assertThat(fetched.byteSize()).isEqualTo(4);
    assertThat(fetched.asNettyBuffer().refCnt()).isEqualTo(2); // 1 for map, 1 for fetch caller
    fetched.release();
  }

  @Test
  @DisplayName("should evict oldest entry (LRU) when max memory limit is exceeded")
  void shouldEvictOldestEntryWhenOom() {
    // given: max is 10 bytes, inserting 3 items of 4 bytes each (12 total)
    final TestKey key1 = new TestKey("k1");
    final TestKey key2 = new TestKey("k2");
    final TestKey key3 = new TestKey("k3");
    final byte[] data = new byte[] {1, 2, 3, 4}; // 4 bytes each
    // when
    storage.store(key1, data); // 4 / 10 bytes
    storage.store(key2, data); // 8 / 10 bytes
    storage.store(key3, data); // 12 / 10 bytes -> eviction
    // then
    assertThat(storage.fetch(key1)).isNull();
    final NativeStoragePayload p2 = storage.fetch(key2);
    final NativeStoragePayload p3 = storage.fetch(key3);
    assertThat(p2).isNotNull();
    assertThat(p3).isNotNull();
    p2.release();
    p3.release();
  }

  @Test
  @DisplayName(
      "should update LRU pointer on fetch and protect recently fetched items from eviction")
  void shouldUpdateLruPointerOnFetch() {
    final TestKey key1 = new TestKey("k1");
    final TestKey key2 = new TestKey("k2");
    final TestKey key3 = new TestKey("k3");
    final byte[] data = new byte[] {1, 2, 3, 4};
    storage.store(key1, data); // older
    storage.store(key2, data); // newer
    // when: fetch key1, moving it to the front of LRU (making key2 the oldest)
    final NativeStoragePayload p1 = storage.fetch(key1);
    p1.release();
    // insert key3 causing eviction
    storage.store(key3, data);
    // then: key2 should be evicted, key1 should survive
    assertThat(storage.fetch(key2)).isNull();
    final NativeStoragePayload p1Again = storage.fetch(key1);
    assertThat(p1Again).isNotNull();
    p1Again.release();
  }

  @Test
  @DisplayName("should manually remove a specific key and drop ownership")
  void shouldRemoveSpecificKey() {
    final TestKey key = new TestKey("rem");
    storage.store(key, new byte[] {1});
    // when
    storage.remove(key);
    // then
    assertThat(storage.fetch(key)).isNull();
  }

  @Test
  @DisplayName("should safely empty all contents and release memory")
  void shouldEmptyAllContents() {
    storage.store(new TestKey("1"), new byte[] {1, 2});
    storage.store(new TestKey("2"), new byte[] {3, 4});
    // when
    storage.empty();
    // then
    assertThat(storage.fetch(new TestKey("1"))).isNull();
    assertThat(storage.fetch(new TestKey("2"))).isNull();
  }
}
