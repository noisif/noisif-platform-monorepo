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
package xyz.jwizard.jws.gateway.redis;

import xyz.jwizard.jwl.kv.pubsub.KvChannel;
import xyz.jwizard.jwl.kv.pubsub.subscriber.AbstractKvSubscriber;
import xyz.jwizard.jwl.kv.pubsub.subscriber.SubscriptionMode;
import xyz.jwizard.jwl.websocket.dispatcher.LocalSessionDispatcher;
import xyz.jwizard.jws.gateway.WsKvChannel;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
class RedisTopicWsSubscriber extends AbstractKvSubscriber<byte[]> {
    private final LocalSessionDispatcher localSessionDispatcher;

    @Inject
    RedisTopicWsSubscriber(LocalSessionDispatcher localSessionDispatcher) {
        this.localSessionDispatcher = localSessionDispatcher;
    }

    @Override
    public KvChannel getChannel() {
        return WsKvChannel.TOPIC_RECEIVE_EVENTS;
    }

    @Override
    public Class<byte[]> getPayloadType() {
        return byte[].class;
    }

    @Override
    public SubscriptionMode getMode() {
        return SubscriptionMode.PATTERN;
    }

    @Override
    public void handle(String channel, String[] params, byte[] message) {
        if (params != null && params.length > 0) {
            localSessionDispatcher.dispatchRaw(params[0], message);
        }
    }
}
