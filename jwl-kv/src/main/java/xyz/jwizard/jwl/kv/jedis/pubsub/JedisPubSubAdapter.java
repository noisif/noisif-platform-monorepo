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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.kv.pubsub.pattern.ChannelParamExtractor;
import xyz.jwizard.jwl.kv.pubsub.subscriber.KvSubscriber;

import redis.clients.jedis.JedisPubSub;

public class JedisPubSubAdapter extends JedisPubSub {
    private static final Logger LOG = LoggerFactory.getLogger(JedisPubSubAdapter.class);

    private final KvSubscriber<String> kvSubscriber;
    private final ChannelParamExtractor paramExtractor;

    public JedisPubSubAdapter(KvSubscriber<String> kvSubscriber,
                              ChannelParamExtractor paramExtractor) {
        this.kvSubscriber = kvSubscriber;
        this.paramExtractor = paramExtractor;
    }

    @Override
    public void onMessage(String channel, String message) {
        LOG.debug("KV RECEIVED (pubSub, String) -> channel: '{}'", channel);
        if (kvSubscriber != null) {
            kvSubscriber.handle(channel, new String[0], message);
        }
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        LOG.debug("KV RECEIVED (pattern, byte[]) -> pattern: '{}', channel: '{}'", pattern,
            channel);
        if (kvSubscriber != null) {
            final String[] extractedParams = paramExtractor.extract(channel);
            kvSubscriber.handle(channel, extractedParams, message);
        }
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        if (kvSubscriber != null) {
            kvSubscriber.setSubscribed(true);
            LOG.debug("Successfully subscribed to channel/pattern: '{}' (total active: {})",
                channel, subscribedChannels);
        }
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        LOG.debug("Unsubscribed from channel/pattern: '{}'", channel);
    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
        onSubscribe(pattern, subscribedChannels);
    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {
        onUnsubscribe(pattern, subscribedChannels);
    }
}
