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
package xyz.noisif.nsl.zcache;

import xyz.noisif.nsl.zcache.offheap.OffHeapPayload;

public class Node {
  private final NativeStorageKey key;
  private final OffHeapPayload payload;
  private Node prev;
  private Node next;

  public Node(NativeStorageKey key, OffHeapPayload payload) {
    this.key = key;
    this.payload = payload;
  }

  public NativeStorageKey getKey() {
    return key;
  }

  public OffHeapPayload getPayload() {
    return payload;
  }

  public Node getPrev() {
    return prev;
  }

  public Node getNext() {
    return next;
  }

  public void setPrev(Node prev) {
    this.prev = prev;
  }

  public void setNext(Node next) {
    this.next = next;
  }
}
