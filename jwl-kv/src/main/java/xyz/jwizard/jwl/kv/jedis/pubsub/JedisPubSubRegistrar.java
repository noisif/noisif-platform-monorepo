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

import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.util.io.RunnableWithException;
import xyz.jwizard.jwl.common.util.thread.ThreadUtil;
import xyz.jwizard.jwl.kv.jedis.pubsub.pattern.RegexChannelParamExtractor;
import xyz.jwizard.jwl.kv.pubsub.PubSubRegistrar;
import xyz.jwizard.jwl.kv.pubsub.pattern.ChannelParamExtractor;
import xyz.jwizard.jwl.kv.pubsub.subscriber.KvSubscriber;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisPubSubRegistrar implements PubSubRegistrar {
    private static final Logger LOG = LoggerFactory.getLogger(JedisPubSubRegistrar.class);

    private final UnifiedJedis redisClient;

    public JedisPubSubRegistrar(UnifiedJedis redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public void subscribe(KvSubscriber<String> subscriber) {
        registerString("pub/sub", "kv-sub-", subscriber, redisClient::subscribe);
    }

    @Override
    public void subscribeBinary(KvSubscriber<byte[]> subscriber) {
        registerBinary("binary pub/sub", "kv-sub-bin-", subscriber, redisClient::subscribe);
    }

    @Override
    public void pSubscribe(KvSubscriber<String> subscriber) {
        registerString("pattern pub/sub", "kv-psub-", subscriber, redisClient::psubscribe);
    }

    @Override
    public void pSubscribeBinary(KvSubscriber<byte[]> subscriber) {
        registerBinary("binary pattern pub/sub", "kv-psub-bin-", subscriber,
            redisClient::psubscribe);
    }

    private void registerString(String logType, String threadPrefix,
                                KvSubscriber<String> subscriber,
                                BiConsumer<JedisPubSub, String[]> jedisAction) {
        final String channelOrPattern = buildChannelName(subscriber);
        final ChannelParamExtractor extractor = new RegexChannelParamExtractor(channelOrPattern);
        registerAsync(logType, threadPrefix, channelOrPattern, () ->
            jedisAction.accept(
                new JedisPubSubAdapter(subscriber, extractor),
                new String[]{channelOrPattern}
            )
        );
    }

    private void registerBinary(String logType, String threadPrefix,
                                KvSubscriber<byte[]> subscriber,
                                BiConsumer<BinaryJedisPubSub, byte[][]> jedisAction) {
        final String channelOrPattern = buildChannelName(subscriber);
        final byte[] channelBytes = channelOrPattern.getBytes(StandardCharsets.UTF_8);
        final ChannelParamExtractor extractor = new RegexChannelParamExtractor(channelOrPattern);
        registerAsync(logType, threadPrefix, channelOrPattern, () ->
            jedisAction.accept(
                new BinaryJedisPubSubAdapter(subscriber, extractor),
                new byte[][]{channelBytes}
            )
        );
    }

    private String buildChannelName(KvSubscriber<?> subscriber) {
        return subscriber.getChannel().buildChannel(subscriber.getChannelParams());
    }

    private void registerAsync(String logType, String threadPrefix, String channelOrPattern,
                               RunnableWithException redisAction) {
        LOG.debug("Registering {} listener on: '{}'", logType, channelOrPattern);
        ThreadUtil.runAsync(threadPrefix + channelOrPattern, () -> {
            try {
                redisAction.run();
                // catch only connection shutdown - let everything else bubble up
                // to ThreadUtil logger
            } catch (JedisConnectionException ex) {
                LOG.debug("Connection closed for pub/sub listener {}: '{}'", logType,
                    channelOrPattern);
            }
        });
    }
}
