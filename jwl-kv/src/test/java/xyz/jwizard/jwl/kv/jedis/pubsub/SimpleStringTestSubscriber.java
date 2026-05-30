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
package xyz.jwizard.jwl.kv.jedis.pubsub;

import xyz.jwizard.jwl.kv.pubsub.KvChannel;
import xyz.jwizard.jwl.kv.pubsub.TestKvChannel;
import xyz.jwizard.jwl.kv.pubsub.subscriber.AbstractKvSubscriber;

import jakarta.inject.Singleton;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class SimpleStringTestSubscriber extends AbstractKvSubscriber<String> {
  private CountDownLatch latch;
  private AtomicReference<String> receivedRef;

  public void prepareForTest(CountDownLatch latch, AtomicReference<String> receivedRef) {
    this.latch = latch;
    this.receivedRef = receivedRef;
  }

  @Override
  public KvChannel getChannel() {
    return TestKvChannel.TEST_EVENTS;
  }

  @Override
  public Class<String> getPayloadType() {
    return String.class;
  }

  @Override
  public void handle(String channel, String[] params, String message) {
    if (receivedRef != null) {
      receivedRef.set(message);
    }
    if (latch != null) {
      latch.countDown();
    }
  }
}
