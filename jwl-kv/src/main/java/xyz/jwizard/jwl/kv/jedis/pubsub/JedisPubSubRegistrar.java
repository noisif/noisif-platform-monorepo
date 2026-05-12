/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
