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
package xyz.noisif.nss.gateway;

import xyz.noisif.nsl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.reflect.ClassScanner;
import xyz.noisif.nsl.kv.KeyValueStore;
import xyz.noisif.nsl.kv.KvServer;
import xyz.noisif.nsl.kv.jedis.JedisServer;
import xyz.noisif.nsl.kv.jedis.factory.FactoryType;
import xyz.noisif.nsl.kv.pubsub.PubSubBroadcaster;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Set;

@Singleton
class KvServerLifecycle implements LifecycleHook {
  private final KvServer kvServer;

  @Inject
  KvServerLifecycle(ComponentProvider componentProvider) {
    kvServer =
        JedisServer.builder()
            .rawNodes(Set.of("127.0.0.1:9113" /* TODO: getting from config server */))
            .password(null /* TODO: getting from config server */)
            .poolMaxTotal(128 /* TODO: getting from config server */)
            .poolMinIdle(16 /* TODO: getting from config server */)
            .poolMaxIdle(64 /* TODO: getting from config server */)
            .componentProvider(componentProvider)
            .withFactory(FactoryType.SINGLE_NODE)
            .build();
  }

  @Override
  public void onStart(ComponentProvider componentProvider, ClassScanner scanner) {
    kvServer.start();
  }

  @Override
  public void onStop() {
    kvServer.close();
  }

  @Produces
  @Singleton
  KeyValueStore keyValueStore() {
    return kvServer;
  }

  @Produces
  @Singleton
  PubSubBroadcaster pubSubBroadcaster() {
    return kvServer;
  }
}
