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
package xyz.jwizard.jws.gateway;

import java.time.Duration;
import java.util.List;

import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializerRegistry;
import xyz.jwizard.jwl.codec.serialization.json.JacksonSerializer;
import xyz.jwizard.jwl.codec.serialization.protobuf.ProtobufSerializer;
import xyz.jwizard.jwl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.limit.TokenBucketRateLimiter;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.kv.pubsub.PubSubBroadcaster;
import xyz.jwizard.jwl.net.envelope.ActionGroup;
import xyz.jwizard.jwl.websocket.WsServer;
import xyz.jwizard.jwl.websocket.auth.WsTokenAuthenticator;
import xyz.jwizard.jwl.websocket.broadcast.WsBroadcaster;
import xyz.jwizard.jwl.websocket.dispatcher.ConcurrentLocalSessionDispatcher;
import xyz.jwizard.jwl.websocket.dispatcher.LocalSessionDispatcher;
import xyz.jwizard.jwl.websocket.jetty.JettyWsServer;
import xyz.jwizard.jwl.websocket.listener.ActionRouterWsMessageListener;
import xyz.jwizard.jwl.websocket.negotation.QueryParamSerializerResolver;
import xyz.jwizard.jwl.websocket.registry.InMemoryWsSessionRegistry;
import xyz.jwizard.jwl.websocket.registry.WsSubscriptionRegistry;
import xyz.jwizard.jws.gateway.redis.RedisWsMessageSink;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
class WsServerLifecycle implements LifecycleHook {
    private final WsServer wsServer;

    @Inject
    WsServerLifecycle(ComponentProvider componentProvider, PubSubBroadcaster pubSubBroadcaster,
                      ClassScanner scanner) {
        wsServer = JettyWsServer.builder()
            .port(9016) /* TODO: incoming from config server */
            .path("/v1") /* TODO: incoming from config server */
            .idleTimeout(Duration.ofMinutes(10))
            .sessionRegistry(InMemoryWsSessionRegistry.createDefault())
            .addAuthenticator(WsTokenAuthenticator.builder()
                .expectedToken("TEST_TOKEN") /* TODO: incoming from config server */
                .principalId("jws-gateway")
                .withQueryParameterCheck("token")
                .build()
            )
            .componentProvider(componentProvider)
            .rateLimiter(TokenBucketRateLimiter.createDefault())
            .serializerRegistry(EnvelopeSerializerRegistry.createEnvelopeRegistry()
                .registerJsonDefaults(JacksonSerializer.createLenientForMessaging())
                .registerProtobufDefaults(ProtobufSerializer.createDefault(scanner))
            )
            .serializerResolverFactory(registry -> QueryParamSerializerResolver.builder()
                .registry(registry)
                .encodingParamName("encoding")
                .frameParamName("frame")
                .build())
            .addBusListener(ActionRouterWsMessageListener.builder()
                .actionGroup(ActionGroup.GLOBAL)
                .componentProvider(componentProvider)
                .build()
            )
            .localSessionDispatcherFactory(ConcurrentLocalSessionDispatcher::createVirtual)
            .messageSink(RedisWsMessageSink.createDefault(pubSubBroadcaster))
            .build();
    }

    @Override
    public void onStart(ComponentProvider componentProvider, ClassScanner scanner) {
        wsServer.start();
    }

    @Override
    public void onStop() {
        wsServer.close();
    }

    @Override
    public List<Class<? extends LifecycleHook>> dependsOn() {
        return List.of(KvServerLifecycle.class, QueueServerLifecycle.class);
    }

    @Produces
    @Singleton
    WsSubscriptionRegistry wsSubscriptionRegistry() {
        return wsServer.getWsSubscriptionRegistry();
    }

    @Produces
    @Singleton
    WsBroadcaster wsBroadcaster() {
        return wsServer.getBroadcaster();
    }

    @Produces
    @Singleton
    LocalSessionDispatcher localSessionDispatcher() {
        return wsServer.getLocalSessionDispatcher();
    }
}
