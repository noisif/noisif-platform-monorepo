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

import xyz.jwizard.jwl.kv.pubsub.PubSubBroadcaster;
import xyz.jwizard.jwl.websocket.broadcast.WsMessageSink;
import xyz.jwizard.jwl.websocket.broadcast.WsTopic;
import xyz.jwizard.jws.gateway.WsKvChannel;

public class RedisWsMessageSink implements WsMessageSink {
    private final PubSubBroadcaster pubSubBroadcaster;

    private RedisWsMessageSink(PubSubBroadcaster pubSubBroadcaster) {
        this.pubSubBroadcaster = pubSubBroadcaster;
    }

    public static RedisWsMessageSink createDefault(PubSubBroadcaster pubSubBroadcaster) {
        return new RedisWsMessageSink(pubSubBroadcaster);
    }

    @Override
    public void payload(WsTopic topic, byte[] payload) {
        pubSubBroadcaster.publishBinary(WsKvChannel.TOPIC_BROADCAST, payload, topic.getTopic());
    }

    @Override
    public void payloadAll(byte[] payload) {
        pubSubBroadcaster.publishBinary(WsKvChannel.GLOBAL_BROADCAST, payload);
    }
}
