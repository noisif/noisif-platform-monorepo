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
package xyz.noisif.nsl.kv.pubsub.subscriber;

import xyz.noisif.nsl.kv.pubsub.KvChannel;

public interface KvSubscriber<T> {
  KvChannel getChannel();

  Class<T> getPayloadType();

  boolean isSubscribed();

  void setSubscribed(boolean subscribed);

  default Object[] getChannelParams() {
    return new Object[0];
  }

  // for pattern subscribers
  default SubscriptionMode getMode() {
    return SubscriptionMode.EXACT;
  }

  void handle(String channel, String[] params, T message);
}
