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
package xyz.noisif.nsl.queue;

import xyz.noisif.nsl.codec.serialization.SerializerFormat;
import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryCountingListener implements QueueListener<byte[]> {
  private final CountDownLatch latch;
  private final AtomicInteger attempts = new AtomicInteger(0);
  private final QueueTopology topology;

  public RetryCountingListener(int maxRetries) {
    latch = new CountDownLatch(maxRetries + 1);
    topology =
        QueueTopology.builder()
            .withDeadLetter()
            .withRetries(maxRetries, 200) // 200ms
            .build();
  }

  @Override
  public String getQueueName() {
    return "test.retry.queue";
  }

  @Override
  public Class<byte[]> getMessageType() {
    return byte[].class;
  }

  @Override
  public SerializerFormat getFormat() {
    return StandardSerializerFormat.RAW;
  }

  @Override
  public void onMessage(byte[] message) {
    attempts.incrementAndGet();
    latch.countDown();
    throw new RuntimeException("Intentional failure for retry test");
  }

  @Override
  public QueueTopology getTopology() {
    return topology;
  }

  public CountDownLatch getLatch() {
    return latch;
  }

  public int getAttempts() {
    return attempts.get();
  }
}
