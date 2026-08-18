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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.noisif.nsl.common.util.StringUtil;

import io.netty.buffer.ByteBuf;

class OffHeapPayloadTest {
  @Test
  @DisplayName("should allocate off-heap Netty buffer and return valid size")
  void shouldAllocateAndReturnSize() {
    // given
    final byte[] data = StringUtil.getBytes("noisif");
    // when
    final OffHeapPayload payload = new OffHeapPayload(data);
    // then
    assertThat(payload.byteSize()).isEqualTo(6);
    // cleanup
    payload.release();
  }

  @Test
  @DisplayName("should convert to raw Netty ByteBuf and contain original data")
  void shouldConvertToRawNettyBuffer() {
    // given
    final byte[] data = StringUtil.getBytes("hello Netty");
    final OffHeapPayload payload = new OffHeapPayload(data);
    // when
    final ByteBuf buffer = payload.asNettyBuffer();
    // then
    assertThat(buffer.refCnt()).isEqualTo(1);
    final byte[] readData = new byte[buffer.readableBytes()];
    buffer.readBytes(readData);
    assertThat(StringUtil.create(readData)).isEqualTo("hello Netty");
    // cleanup
    payload.release();
  }

  @Test
  @DisplayName("should correctly bump reference count and handle release")
  void shouldHandleReferenceCounting() {
    // given
    final OffHeapPayload payload = new OffHeapPayload(new byte[] {1, 2, 3});
    // when
    final boolean retained = payload.tryRetain(); // bump refCnt to 2
    // then
    assertThat(retained).isTrue();
    assertThat(payload.asNettyBuffer().refCnt()).isEqualTo(2);
    // cleanup
    payload.release(); // down to 1
    assertThat(payload.asNettyBuffer().refCnt()).isEqualTo(1);
    payload.release(); // down to 0, memory freed
  }

  @Test
  @DisplayName("should safely return false on tryRetain if buffer is already freed")
  void shouldFailTryRetainOnFreedBuffer() {
    // given
    final OffHeapPayload payload = new OffHeapPayload(new byte[] {1, 2, 3});
    payload.release(); // refCnt is now 0, netty destroyed it
    // when
    final boolean retained = payload.tryRetain(); // should fail gracefully
    // then
    assertThat(retained).isFalse();
  }
}
