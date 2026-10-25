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
package xyz.noisif.nss.api.kv

import xyz.noisif.nsl.common.di.ComponentProvider
import xyz.noisif.nsl.kv.jedis.JedisServer
import xyz.noisif.nsl.kv.jedis.factory.FactoryType
import xyz.noisif.nsl.kv.pubsub.PubSubBroadcaster
import xyz.noisif.nsl.kv.{KeyValueStore, KvServer}

import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton

import java.util.{Set => JSet}

@Singleton
class KvServerConfiguration {
  @Produces
  @Singleton
  def kvServer(componentProvider: ComponentProvider): KvServer = JedisServer
    .builder()
    .rawNodes(JSet.of("127.0.0.1:9113" /* TODO: getting from config server */ ))
    .password(null /* TODO: getting from config server */ )
    .poolMaxTotal(128 /* TODO: getting from config server */ )
    .poolMinIdle(16 /* TODO: getting from config server */ )
    .poolMaxIdle(64 /* TODO: getting from config server */ )
    .componentProvider(componentProvider)
    .withFactory(FactoryType.SINGLE_NODE)
    .build()

  @Produces
  @Singleton
  def keyValueStore(kvServer: KvServer): KeyValueStore = kvServer

  @Produces
  @Singleton
  def pubSubBroadcaster(kvServer: KvServer): PubSubBroadcaster = kvServer
}
