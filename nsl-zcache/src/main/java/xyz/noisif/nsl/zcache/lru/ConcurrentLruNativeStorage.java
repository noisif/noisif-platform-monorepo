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

import xyz.noisif.nsl.common.util.CollectionUtil;
import xyz.noisif.nsl.zcache.GenericNativeStorage;
import xyz.noisif.nsl.zcache.NativeStorageKey;
import xyz.noisif.nsl.zcache.NativeStoragePayload;
import xyz.noisif.nsl.zcache.Node;
import xyz.noisif.nsl.zcache.offheap.OffHeapPayload;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentLruNativeStorage extends GenericNativeStorage {
  private long currentBytes = 0;

  // lock-free reads for ultra low latency
  private final ConcurrentMap<NativeStorageKey, Node> map;

  // lock is only used to sync the lru linked list pointers and eviction
  private final ReentrantLock lock = new ReentrantLock();

  // dummy boundaries
  private final Node head = new Node(null, null);
  private final Node tail = new Node(null, null);

  private ConcurrentLruNativeStorage(Builder builder) {
    super(builder);
    map = CollectionUtil.createWithInitSize(initialCapacity);
    head.setNext(tail);
    tail.setPrev(head);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void store(NativeStorageKey key, byte[] rawData) {
    final OffHeapPayload payload = new OffHeapPayload(rawData);
    final Node newNode = new Node(key, payload);
    lock.lock();
    try {
      final Node existing = map.put(key, newNode);
      if (existing != null) {
        // overwrite scenario
        removeNode(existing);
        currentBytes -= existing.getPayload().byteSize();
        existing.getPayload().release();
      }
      addNodeToHead(newNode);
      currentBytes += payload.byteSize();
      // evict oldest stuff if we hit our ram budget
      while (currentBytes > maxBytes && tail.getPrev() != head) {
        final Node lru = tail.getPrev();
        removeNode(lru);
        map.remove(lru.getKey());
        currentBytes -= lru.getPayload().byteSize();
        // zcache gives up ownership. if Netty is sending it, it survives until Netty is done
        lru.getPayload().release();
        log.debug("evicted native memory block for key: {}", lru.getKey().getKeyName());
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public NativeStoragePayload fetch(NativeStorageKey key) {
    final Node node = map.get(key);
    if (node == null) {
      return null;
    }
    // atomic bump. if it fails, eviction just grabbed it, treat as cache miss
    if (!node.getPayload().tryRetain()) {
      return null;
    }
    // try to bump lru position without blocking the current thread
    if (lock.tryLock()) {
      try {
        if (node.getPrev() != null && node.getNext() != null) {
          removeNode(node);
          addNodeToHead(node);
        }
      } finally {
        lock.unlock();
      }
    }
    // safe to return, caller must call release()
    return node.getPayload();
  }

  @Override
  public void remove(NativeStorageKey key) {
    lock.lock();
    try {
      final Node node = map.remove(key);
      if (node != null) {
        removeNode(node);
        currentBytes -= node.getPayload().byteSize();
        // zcache drops ownership. ram is freed unless Netty is currently sending it
        node.getPayload().release();
        log.debug("manually removed key from zcache: {}", key);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void remove(NativeStorageKey... keys) {
    if (keys == null || keys.length == 0) {
      return;
    }
    // batch operation under a single lock to avoid rapid lock contention
    lock.lock();
    try {
      int removedCount = 0;
      for (final NativeStorageKey key : keys) {
        final Node node = map.remove(key);
        if (node != null) {
          removeNode(node);
          currentBytes -= node.getPayload().byteSize();
          node.getPayload().release();
          removedCount++;
        }
      }
      if (removedCount > 0) {
        log.debug("manually removed {} keys from zcache in batch", removedCount);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void empty() {
    lock.lock();
    try {
      for (final Node node : map.values()) {
        node.getPayload().release();
      }
      map.clear();
      head.setNext(tail);
      tail.setPrev(head);
      currentBytes = 0;
      log.info("zcache shattered, native memory freed");
    } finally {
      lock.unlock();
    }
  }

  private void addNodeToHead(Node node) {
    node.setNext(head.getNext());
    node.setPrev(head);
    head.getNext().setPrev(node);
    head.setNext(node);
  }

  private void removeNode(Node node) {
    node.getPrev().setNext(node.getNext());
    node.getNext().setPrev(node.getPrev());
    node.setPrev(null);
    node.setNext(null);
  }

  public static class Builder extends AbstractBuilder<Builder> {
    private Builder() {}

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public ConcurrentLruNativeStorage build() {
      validate();
      return new ConcurrentLruNativeStorage(this);
    }
  }
}
