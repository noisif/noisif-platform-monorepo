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
package xyz.noisif.nss.gateway.ws;

import xyz.noisif.nsl.codec.envelope.EnvelopeSerializerRegistry;
import xyz.noisif.nsl.codec.serialization.json.jackson.JacksonSerializer;
import xyz.noisif.nsl.codec.serialization.protobuf.ProtobufSerializer;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.limit.TokenBucketRateLimiter;
import xyz.noisif.nsl.common.reflect.ClassScanner;
import xyz.noisif.nsl.kv.pubsub.PubSubBroadcaster;
import xyz.noisif.nsl.net.envelope.ActionGroup;
import xyz.noisif.nsl.websocket.WsServer;
import xyz.noisif.nsl.websocket.auth.WsTokenAuthenticator;
import xyz.noisif.nsl.websocket.broadcast.WsBroadcaster;
import xyz.noisif.nsl.websocket.dispatcher.ConcurrentLocalSessionDispatcher;
import xyz.noisif.nsl.websocket.dispatcher.LocalSessionDispatcher;
import xyz.noisif.nsl.websocket.jetty.JettyWsServer;
import xyz.noisif.nsl.websocket.listener.ActionRouterWsMessageListener;
import xyz.noisif.nsl.websocket.negotation.QueryParamSerializerResolver;
import xyz.noisif.nsl.websocket.registry.InMemoryWsSessionRegistry;
import xyz.noisif.nsl.websocket.registry.WsSessionRegistry;
import xyz.noisif.nsl.websocket.registry.WsSubscriptionRegistry;
import xyz.noisif.nss.gateway.kv.ws.RedisWsMessageSink;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.time.Duration;

@Singleton
class WsServerConfiguration {
  @Produces
  @Singleton
  WsSessionRegistry wsSessionRegistry() {
    return InMemoryWsSessionRegistry.createDefault();
  }

  @Produces
  @Singleton
  WsServer wsServer(
      WsSessionRegistry wsSessionRegistry,
      ComponentProvider componentProvider,
      PubSubBroadcaster pubSubBroadcaster,
      ClassScanner classScanner) {
    return JettyWsServer.builder()
        .port(9210)
        .path("/v1")
        .idleTimeout(Duration.ofMinutes(10))
        .sessionRegistry(wsSessionRegistry)
        .addAuthenticator(
            WsTokenAuthenticator.builder()
                .expectedToken("TEST_TOKEN")
                .principalId("nss-gateway")
                .withQueryParameterCheck("token")
                .build())
        .componentProvider(componentProvider)
        .rateLimiter(TokenBucketRateLimiter.createDefault())
        .serializerRegistry(
            EnvelopeSerializerRegistry.createEnvelopeRegistry()
                .registerJsonDefaults(JacksonSerializer.createLenientForMessaging())
                .registerProtobufDefaults(ProtobufSerializer.createDefault(classScanner)))
        .serializerResolverFactory(
            registry ->
                QueryParamSerializerResolver.builder()
                    .registry(registry)
                    .encodingParamName("encoding")
                    .frameParamName("frame")
                    .build())
        .addBusListener(
            ActionRouterWsMessageListener.builder()
                .actionGroup(ActionGroup.GLOBAL)
                .componentProvider(componentProvider)
                .build())
        .localSessionDispatcherFactory(ConcurrentLocalSessionDispatcher::createVirtual)
        .messageSink(RedisWsMessageSink.createDefault(pubSubBroadcaster))
        .build();
  }

  @Produces
  @Singleton
  WsSubscriptionRegistry wsSubscriptionRegistry(WsSessionRegistry wsSessionRegistry) {
    return wsSessionRegistry;
  }

  @Produces
  @Singleton
  WsBroadcaster wsBroadcaster(WsServer wsServer) {
    return wsServer.getBroadcaster();
  }

  @Produces
  @Singleton
  LocalSessionDispatcher localSessionDispatcher(WsServer wsServer) {
    return wsServer.getLocalSessionDispatcher();
  }
}
