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
package xyz.noisif.nsl.zcache.offheap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.zcache.NativeStoragePayload;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.IllegalReferenceCountException;

public class OffHeapPayload implements NativeStoragePayload {
  private static final Logger LOG = LoggerFactory.getLogger(OffHeapPayload.class);

  private final ByteBuf buffer;
  private final int size;

  public OffHeapPayload(byte[] data) {
    size = data.length;
    // grab off-heap memory from Netty's pool, acts like a fast malloc
    buffer = PooledByteBufAllocator.DEFAULT.directBuffer(size);
    buffer.writeBytes(data);
  }

  @Override
  public ByteBuf asNettyBuffer() {
    return buffer;
  }

  @Override
  public void release() {
    // netty frees the native memory under the hood when ref count hits 0
    final boolean isFreed = buffer.release();
    if (isFreed) {
      LOG.trace("off-heap payload memory successfully released back to the pool");
    }
  }

  @Override
  public int byteSize() {
    return size;
  }

  public boolean tryRetain() {
    try {
      buffer.retain(); // bump ref count by 1
      return true;
    } catch (IllegalReferenceCountException ex) {
      LOG.debug("failed to retain buffer, already freed or pending destruction");
      return false;
    }
  }
}
