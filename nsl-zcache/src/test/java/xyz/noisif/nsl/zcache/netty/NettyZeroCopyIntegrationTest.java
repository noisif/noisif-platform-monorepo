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
package xyz.noisif.nsl.zcache.netty;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.noisif.nsl.common.util.StringUtil;
import xyz.noisif.nsl.common.util.math.MemUnit;
import xyz.noisif.nsl.zcache.GenericNativeStorage;
import xyz.noisif.nsl.zcache.NativeStoragePayload;
import xyz.noisif.nsl.zcache.TestKey;
import xyz.noisif.nsl.zcache.lru.ConcurrentLruNativeStorage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;

class NettyZeroCopyIntegrationTest {
  private GenericNativeStorage storage;

  @BeforeEach
  void setUp() {
    storage =
        ConcurrentLruNativeStorage.builder().maxMemory(10, MemUnit.MB).initialCapacity(16).build();
    storage.start();
  }

  @AfterEach
  void tearDown() {
    storage.close();
  }

  @Test
  @DisplayName(
      "should send payload through Netty pipeline with true Zero-Copy (same memory address)")
  void shouldSendPayloadWithZeroCopy() {
    // given
    final TestKey key = new TestKey("video_chunk_1");
    final byte[] rawData = StringUtil.getBytes("simulated_binary_data");
    storage.store(key, rawData);

    // fetch from the zcache
    final NativeStoragePayload payload = storage.fetch(key);
    final ByteBuf originalBuffer = payload.asNettyBuffer();

    // ensure this is direct memory (off-heap), which is required for os-level zero-copy
    assertThat(originalBuffer.isDirect()).isTrue();
    // record the native memory address of our buffer
    final long originalMemoryAddress = originalBuffer.memoryAddress();
    // simulate a netty network connection
    final EmbeddedChannel channel = new EmbeddedChannel();

    // when
    // if we send the original buffer, Netty will empty it, ruining the content for subsequent cache
    // reads, duplicate() creates a new java object, but it points to the exact same native memory
    final ByteBuf toSend = originalBuffer.retainedDuplicate();

    // send outbound (netty consumes 1 reference after a successful write to the socket)
    channel.writeOutbound(toSend);

    // then
    final ByteBuf outboundBuffer = channel.readOutbound();
    // prove zero-copy: the outbound memory address must be identical to the zcache address
    assertThat(outboundBuffer.memoryAddress()).isEqualTo(originalMemoryAddress);

    // verify the data survived intact
    final byte[] receivedBytes = new byte[outboundBuffer.readableBytes()];
    outboundBuffer.readBytes(receivedBytes);
    assertThat(StringUtil.create(receivedBytes)).isEqualTo("simulated_binary_data");

    // simulation: netty releases the buffer after sending the packet over the network
    outboundBuffer.release();
    payload.release();

    // the zcache still holds its own reference count, so the memory was not freed,
    // and the readerIndex in the zcache remains intact (it is 0)
    assertThat(originalBuffer.refCnt()).isEqualTo(1);
    assertThat(originalBuffer.readerIndex()).isZero();
  }
}
