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

import xyz.noisif.nsl.codec.serialization.SerializerRegistry;
import xyz.noisif.nsl.codec.serialization.json.JacksonSerializer;
import xyz.noisif.nsl.codec.serialization.raw.RawByteSerializer;
import xyz.noisif.nsl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.reflect.ClassScanner;
import xyz.noisif.nsl.queue.MessagePublisher;
import xyz.noisif.nsl.queue.QueueServer;
import xyz.noisif.nsl.queue.rabbitmq.RabbitMqServer;
import xyz.noisif.nsl.queue.rabbitmq.connector.ConnectorType;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Set;

@Singleton
class QueueServerLifecycle implements LifecycleHook {
  private final QueueServer queueServer;

  @Inject
  QueueServerLifecycle(ComponentProvider componentProvider) {
    queueServer =
        RabbitMqServer.builder()
            .rawNodes(Set.of("localhost:9111") /* TODO: incoming from config server */)
            .withConnector(ConnectorType.SINGLE_NODE)
            .username("guest" /* TODO: incoming from config server */)
            .password("guest" /* TODO: incoming from config server */)
            .virtualHost("noisif-main" /* TODO: incoming from config server */)
            .serializerRegistry(
                SerializerRegistry.createDefault()
                    .register(JacksonSerializer.createLenientForMessaging())
                    .register(RawByteSerializer.createDefault()))
            .componentProvider(componentProvider)
            .build();
  }

  @Override
  public void onStart(ComponentProvider componentProvider, ClassScanner scanner) {
    queueServer.start();
  }

  @Override
  public void onStop() {
    queueServer.close();
  }

  @Produces
  @Singleton
  MessagePublisher messagePublisher() {
    return queueServer.getQueuePublisher();
  }
}
