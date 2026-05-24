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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import xyz.jwizard.jwl.kv.pubsub.KvChannel;
import xyz.jwizard.jwl.kv.pubsub.TestKvChannel;
import xyz.jwizard.jwl.kv.pubsub.subscriber.AbstractKvSubscriber;
import xyz.jwizard.jwl.kv.pubsub.subscriber.SubscriptionMode;

import jakarta.inject.Singleton;

@Singleton
public class PatternStringTestSubscriber extends AbstractKvSubscriber<String> {
    private CountDownLatch latch;
    private AtomicReference<String> receivedRef;
    private AtomicReference<String[]> paramsRef;

    public void prepareForTest(CountDownLatch latch,
                               AtomicReference<String> receivedRef,
                               AtomicReference<String[]> paramsRef) {
        this.latch = latch;
        this.receivedRef = receivedRef;
        this.paramsRef = paramsRef;
    }

    @Override
    public KvChannel getChannel() {
        return TestKvChannel.USER_EVENTS_WILDCARD;
    }

    @Override
    public Class<String> getPayloadType() {
        return String.class;
    }

    @Override
    public SubscriptionMode getMode() {
        return SubscriptionMode.PATTERN;
    }

    @Override
    public void handle(String channel, String[] params, String message) {
        if (paramsRef != null) {
            paramsRef.set(params);
        }
        if (receivedRef != null) {
            receivedRef.set(message);
        }
        if (latch != null) {
            latch.countDown();
        }
    }
}
