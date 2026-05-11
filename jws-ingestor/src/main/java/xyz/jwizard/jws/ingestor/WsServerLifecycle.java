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
package xyz.jwizard.jws.ingestor;

import java.time.Duration;
import java.util.List;

import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializerRegistry;
import xyz.jwizard.jwl.codec.envelope.protobuf.ProtobufEnvelopeSerializer;
import xyz.jwizard.jwl.codec.serialization.json.JacksonSerializer;
import xyz.jwizard.jwl.codec.serialization.protobuf.ProtobufSerializer;
import xyz.jwizard.jwl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.limit.TokenBucketRateLimiter;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.websocket.WsServer;
import xyz.jwizard.jwl.websocket.auth.WsTokenAuthenticator;
import xyz.jwizard.jwl.websocket.broadcast.WsBroadcaster;
import xyz.jwizard.jwl.websocket.dispatcher.ConcurrentLocalSessionDispatcher;
import xyz.jwizard.jwl.websocket.jetty.JettyWsServer;
import xyz.jwizard.jwl.websocket.listener.action.ActionRouterWsMessageListener;
import xyz.jwizard.jwl.websocket.negotation.QueryParamSerializerResolver;
import xyz.jwizard.jwl.websocket.registry.InMemoryWsSessionRegistry;
import xyz.jwizard.jwl.websocket.registry.WsSubscriptionRegistry;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
class WsServerLifecycle implements LifecycleHook {
    private final WsServer wsServer;

    @Inject
    WsServerLifecycle(ComponentProvider componentProvider, ClassScanner scanner) {
        wsServer = JettyWsServer.builder()
            .port(9092) /* TODO: incoming from config server */
            .path("/v1") /* TODO: incoming from config server */
            .idleTimeout(Duration.ofMinutes(10))
            .sessionRegistry(InMemoryWsSessionRegistry.createDefault())
            .addAuthenticator(WsTokenAuthenticator.builder()
                .expectedToken("TEST_TOKEN") /* TODO: incoming from config server */
                .principalId("jws-ingestor")
                .withQueryParameterCheck("token")
                .build()
            )
            .componentProvider(componentProvider)
            .rateLimiter(TokenBucketRateLimiter.createDefault())
            .serializerRegistry(EnvelopeSerializerRegistry.createEnvelopeRegistry()
                .registerJsonDefaults(JacksonSerializer.createLenientForMessaging())
                .register(ProtobufEnvelopeSerializer
                    .createDefault(ProtobufSerializer.createDefault(scanner)))
            )
            .serializerResolverFactory(registry -> QueryParamSerializerResolver.builder()
                .registry(registry)
                .encodingParamName("encoding")
                .frameParamName("frame")
                .build())
            .addMessageListener(ActionRouterWsMessageListener.builder()
                .componentProvider(componentProvider)
                .pool(null)
                .build()
            )
            .localSessionDispatcherFactory(ConcurrentLocalSessionDispatcher::createVirtual)
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
        return List.of(GraphServerLifecycle.class, JsEngineLifecycle.class);
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
}
