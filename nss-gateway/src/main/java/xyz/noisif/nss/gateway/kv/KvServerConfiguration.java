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
package xyz.noisif.nss.gateway.kv;

import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.kv.KeyValueStore;
import xyz.noisif.nsl.kv.KvServer;
import xyz.noisif.nsl.kv.jedis.JedisServer;
import xyz.noisif.nsl.kv.jedis.factory.FactoryType;
import xyz.noisif.nsl.kv.pubsub.PubSubBroadcaster;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.Set;

@Singleton
class KvServerConfiguration {
  @Produces
  @Singleton
  KvServer kvServer(ComponentProvider componentProvider) {
    return JedisServer.builder()
        .rawNodes(Set.of("127.0.0.1:9113" /* TODO: getting from config server */))
        .password(null /* TODO: getting from config server */)
        .poolMaxTotal(128 /* TODO: getting from config server */)
        .poolMinIdle(16 /* TODO: getting from config server */)
        .poolMaxIdle(64 /* TODO: getting from config server */)
        .componentProvider(componentProvider)
        .withFactory(FactoryType.SINGLE_NODE)
        .build();
  }

  @Produces
  @Singleton
  KeyValueStore keyValueStore(KvServer kvServer) {
    return kvServer;
  }

  @Produces
  @Singleton
  PubSubBroadcaster pubSubBroadcaster(KvServer kvServer) {
    return kvServer;
  }
}
