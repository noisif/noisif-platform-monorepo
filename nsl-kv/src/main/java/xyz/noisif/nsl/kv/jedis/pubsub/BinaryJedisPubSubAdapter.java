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
package xyz.noisif.nsl.kv.jedis.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.kv.pubsub.pattern.ChannelParamExtractor;
import xyz.noisif.nsl.kv.pubsub.subscriber.KvSubscriber;

import redis.clients.jedis.BinaryJedisPubSub;

import java.nio.charset.StandardCharsets;

public class BinaryJedisPubSubAdapter extends BinaryJedisPubSub {
  private static final Logger LOG = LoggerFactory.getLogger(BinaryJedisPubSubAdapter.class);

  private final KvSubscriber<byte[]> kvSubscriber;
  private final ChannelParamExtractor paramExtractor;

  public BinaryJedisPubSubAdapter(
      KvSubscriber<byte[]> kvSubscriber, ChannelParamExtractor paramExtractor) {
    this.kvSubscriber = kvSubscriber;
    this.paramExtractor = paramExtractor;
  }

  @Override
  public void onMessage(byte[] channel, byte[] message) {
    final String channelStr = new String(channel, StandardCharsets.UTF_8);
    if (LOG.isDebugEnabled()) {
      LOG.debug("KV RECEIVED (pubSub, byte[]) -> channel: '{}'", channelStr);
    }
    if (kvSubscriber != null) {
      kvSubscriber.handle(channelStr, new String[0], message);
    }
  }

  @Override
  public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
    final String channelStr = new String(channel, StandardCharsets.UTF_8);
    if (LOG.isDebugEnabled()) {
      final String patternStr = new String(pattern, StandardCharsets.UTF_8);
      LOG.debug(
          "KV RECEIVED (pattern, byte[]) -> pattern: '{}', channel: '{}'", patternStr, channelStr);
    }
    if (kvSubscriber != null) {
      final String[] extractedParams = paramExtractor.extract(channelStr);
      kvSubscriber.handle(channelStr, extractedParams, message);
    }
  }

  @Override
  public void onSubscribe(byte[] channel, int subscribedChannels) {
    if (kvSubscriber != null) {
      kvSubscriber.setSubscribed(true);
      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "Successfully subscribed to channel/pattern: '{}' (total active: {})",
            new String(channel, StandardCharsets.UTF_8),
            subscribedChannels);
      }
    }
  }

  @Override
  public void onUnsubscribe(byte[] channel, int subscribedChannels) {
    LOG.debug("Unsubscribed from channel/pattern: '{}'", channel);
  }

  @Override
  public void onPSubscribe(byte[] pattern, int subscribedChannels) {
    onSubscribe(pattern, subscribedChannels);
  }

  @Override
  public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
    onUnsubscribe(pattern, subscribedChannels);
  }
}
