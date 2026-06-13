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
package xyz.noisif.nss.gateway.redis;

import xyz.noisif.nsl.kv.pubsub.KvChannel;
import xyz.noisif.nsl.kv.pubsub.subscriber.AbstractKvSubscriber;
import xyz.noisif.nsl.websocket.dispatcher.LocalSessionDispatcher;
import xyz.noisif.nss.gateway.WsKvChannel;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
class RedisGlobalWsSubscriber extends AbstractKvSubscriber<byte[]> {
  private final LocalSessionDispatcher localSessionDispatcher;

  @Inject
  RedisGlobalWsSubscriber(LocalSessionDispatcher localSessionDispatcher) {
    this.localSessionDispatcher = localSessionDispatcher;
  }

  @Override
  public KvChannel getChannel() {
    return WsKvChannel.GLOBAL_BROADCAST;
  }

  @Override
  public Class<byte[]> getPayloadType() {
    return byte[].class;
  }

  @Override
  public void handle(String channel, String[] params, byte[] message) {
    localSessionDispatcher.dispatchRawAll(message);
  }
}
