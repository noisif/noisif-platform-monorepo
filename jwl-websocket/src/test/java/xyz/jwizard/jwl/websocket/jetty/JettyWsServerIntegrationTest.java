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
package xyz.jwizard.jwl.websocket.jetty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializerRegistry;
import xyz.jwizard.jwl.codec.envelope.MessageEnvelope;
import xyz.jwizard.jwl.codec.serialization.json.JsonSerializer;
import xyz.jwizard.jwl.common.di.ApplicationContext;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.di.GuiceComponentProvider;
import xyz.jwizard.jwl.common.reflect.ClassGraphScanner;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.net.envelope.ActionGroup;
import xyz.jwizard.jwl.net.http.cookie.CommonCookieName;
import xyz.jwizard.jwl.net.http.header.CommonHttpHeaderName;
import xyz.jwizard.jwl.websocket.TestConstants;
import xyz.jwizard.jwl.websocket.WsServer;
import xyz.jwizard.jwl.websocket.auth.WsTokenAuthenticator;
import xyz.jwizard.jwl.websocket.auth.handler.TestWsCookieAuthenticator;
import xyz.jwizard.jwl.websocket.broadcast.TestWsTopic;
import xyz.jwizard.jwl.websocket.dispatcher.ConcurrentLocalSessionDispatcher;
import xyz.jwizard.jwl.websocket.listener.ActionRouterWsMessageListener;
import xyz.jwizard.jwl.websocket.listener.action.TestOpCode;
import xyz.jwizard.jwl.websocket.listener.action.WsOpCode;
import xyz.jwizard.jwl.websocket.negotation.QueryParamSerializerResolver;
import xyz.jwizard.jwl.websocket.registry.InMemoryWsSessionRegistry;
import xyz.jwizard.jwl.websocket.registry.WsSubscriptionRegistry;

class JettyWsServerIntegrationTest {
    private final JsonSerializer jsonSerializer = TestConstants.JSON_SERIALIZER;

    private ClassScanner scanner;
    private WsServer server;
    private int port;

    @BeforeEach
    void startServer() {
        scanner = new ClassGraphScanner("xyz.jwizard.jwl.websocket");
        final InMemoryWsSessionRegistry registry = InMemoryWsSessionRegistry.createDefault();
        final ApplicationContext context = ApplicationContext.create(scanner, Map.of(
            ComponentProvider.class, GuiceComponentProvider.class
        ), Map.of(
            WsSubscriptionRegistry.class, registry
        ));
        final ComponentProvider componentProvider = context.getComponentProvider();
        server = JettyWsServer.builder()
            .port(0)
            .path("/v1")
            .componentProvider(componentProvider)
            .serializerRegistry(EnvelopeSerializerRegistry.createEnvelopeRegistry()
                .registerJsonDefaults(jsonSerializer)
            )
            .sessionRegistry(registry)
            .serializerResolverFactory(reg -> QueryParamSerializerResolver.builder()
                .registry(reg)
                .build()
            )
            .localSessionDispatcherFactory(ConcurrentLocalSessionDispatcher::createVirtual)
            .addAuthenticator(WsTokenAuthenticator.builder()
                .expectedToken(TestConstants.SECRET_TOKEN)
                .principalId(TestConstants.SERVICE_NAME)
                .withQueryParameterCheck("auth_token")
                .build())
            .addAuthenticator(new TestWsCookieAuthenticator())
            .addBusListener(ActionRouterWsMessageListener.builder()
                .actionGroup(ActionGroup.GLOBAL)
                .componentProvider(componentProvider)
                .build()
            )
            .build();
        server.start();
        port = server.getLocalPort();
    }

    @AfterEach
    void stopServer() {
        server.close();
        IoUtil.closeQuietly(scanner);
    }

    @Test
    @DisplayName("should accept websocket connection with proper query params")
    void shouldAcceptConnection() throws Exception {
        // given
        final CompletableFuture<Boolean> opened = new CompletableFuture<>();
        final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        final String url = String.format("ws://localhost:%d/v1?encoding=json&frame=text", port);
        // when
        final WebSocket webSocket = client.newWebSocketBuilder()
            .header(CommonHttpHeaderName.AUTHORIZATION.getCode(), TestConstants.SECRET_TOKEN)
            .buildAsync(URI.create(url), new WebSocket.Listener() {
                @Override
                public void onOpen(WebSocket webSocket) {
                    opened.complete(true);
                    webSocket.request(1);
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    opened.completeExceptionally(error);
                }
            })
            .join();
        // then
        final boolean result = opened.get(5, TimeUnit.SECONDS);
        assertThat(result).isTrue();
        // cleanup
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Done");
    }

    @Test
    @DisplayName("should send heartbeat request and receive heartbeat response from server")
    void shouldHandleHeartbeatRoundTrip() throws Exception {
        // given
        final LinkedBlockingQueue<String> responses = new LinkedBlockingQueue<>();
        final HttpClient client = HttpClient.newBuilder().build();
        final String url = String.format("ws://localhost:%d/v1?encoding=json&frame=text", port);
        final WebSocket webSocket = client.newWebSocketBuilder()
            .header(CommonHttpHeaderName.AUTHORIZATION.getCode(), TestConstants.SECRET_TOKEN)
            .buildAsync(URI.create(url), new WebSocket.Listener() {
                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data,
                                                 boolean last) {
                    responses.add(data.toString());
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }
            }).join();
        // {"op": 65541, "data": null}
        final String heartbeatRequest = jsonSerializer.serialize(new MessageEnvelope<>(
            WsOpCode.HEARTBEAT.getCode(),
            null
        ));
        // when
        webSocket.sendText(heartbeatRequest, true);
        // then
        final String rawResponse = responses.poll(5, TimeUnit.SECONDS);
        assertThat(rawResponse).isNotNull();
        final Map<?, ?> responseEnvelope = jsonSerializer.deserialize(rawResponse, Map.class);
        assertThat(responseEnvelope.get("op")).isEqualTo(WsOpCode.HEARTBEAT.getCode());
        // cleanup
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
    }

    @Test
    @DisplayName("should authenticate via session cookie and handle heartbeat round-trip")
    void shouldAuthenticateAndHandleHeartbeat() throws Exception {
        // given
        final String cookieName = CommonCookieName.SID.getCode();
        final String sessionId = "valid-wizard-123";
        final LinkedBlockingQueue<String> responses = new LinkedBlockingQueue<>();
        final HttpClient client = HttpClient.newBuilder().build();
        final String url = String.format(
            "ws://localhost:%d/v1?encoding=json&frame=text", port);
        final MessageEnvelope<Void> requestEnvelope = new MessageEnvelope<>(
            WsOpCode.HEARTBEAT.getCode(), null);
        final String jsonRequest = jsonSerializer.serialize(requestEnvelope);
        // when
        final WebSocket webSocket = client.newWebSocketBuilder()
            .header("Cookie", cookieName + "=" + sessionId)
            .buildAsync(URI.create(url), new WebSocket.Listener() {
                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data,
                                                 boolean last) {
                    responses.add(data.toString());
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }
            }).get(5, TimeUnit.SECONDS);
        webSocket.sendText(jsonRequest, true);
        // then
        final String rawResponse = responses.poll(5, TimeUnit.SECONDS);
        assertThat(rawResponse)
            .as("Server should respond within timeout")
            .isNotNull();
        final Map<?, ?> responseMap = jsonSerializer.deserialize(rawResponse, Map.class);
        assertThat(responseMap.get("op"))
            .as("Response OP code should match HEARTBEAT")
            .isEqualTo(WsOpCode.HEARTBEAT.getCode());
        // cleanup
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "test-finished").join();
    }

    @Test
    @DisplayName("should reject connection with 401 when no valid auth is provided")
    void shouldRejectUnauthorized() {
        // given
        final HttpClient client = HttpClient.newBuilder().build();
        final String badUrl = String.format("ws://localhost:%d/v1?encoding=json&frame=text", port);
        // when & then
        assertThatThrownBy(() ->
            client.newWebSocketBuilder()
                .buildAsync(URI.create(badUrl), new WebSocket.Listener() {
                })
                .get(5, TimeUnit.SECONDS)
        ).hasCauseInstanceOf(java.io.IOException.class);
        // cleanup
        client.close();
    }

    @Test
    @DisplayName("should broadcast message to multiple subscribers in a topic")
    void shouldBroadcastToMultipleSubscribers() throws Exception {
        // given
        final String cookieName = CommonCookieName.SID.getCode();
        final String url = String.format("ws://localhost:%d/v1?encoding=json&frame=text", port);
        final HttpClient client = HttpClient.newBuilder().build();
        final LinkedBlockingQueue<String> user1Queue = new LinkedBlockingQueue<>();
        final LinkedBlockingQueue<String> user2Queue = new LinkedBlockingQueue<>();
        // when: connect user 1
        final WebSocket wsUser1 = client.newWebSocketBuilder()
            .header("Cookie", cookieName + "=valid-alice")
            .buildAsync(URI.create(url), new WebSocket.Listener() {
                @Override
                public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                    user1Queue.add(data.toString());
                    ws.request(1);
                    return null;
                }
            }).join();
        // when: connect user 2
        final WebSocket wsUser2 = client.newWebSocketBuilder()
            .header("Cookie", cookieName + "=valid-bob")
            .buildAsync(URI.create(url), new WebSocket.Listener() {
                @Override
                public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                    user2Queue.add(data.toString());
                    ws.request(1);
                    return null;
                }
            }).join();
        // given
        final String subscribeReq = jsonSerializer.serialize(new MessageEnvelope<>(
            TestOpCode.SUBSCRIBE.getCode(),
            null
        ));
        // when
        wsUser1.sendText(subscribeReq, true);
        wsUser2.sendText(subscribeReq, true);
        // then
        final String ackAlice = user1Queue.poll(5, TimeUnit.SECONDS);
        final String ackBob = user2Queue.poll(5, TimeUnit.SECONDS);
        assertThat(ackAlice).isNotNull();
        assertThat(ackBob).isNotNull();
        final Map<?, ?> mapAckAlice = jsonSerializer.deserialize(ackAlice, Map.class);
        final Map<?, ?> mapAckBob = jsonSerializer.deserialize(ackBob, Map.class);
        assertThat(mapAckAlice.get("op")).isEqualTo(TestOpCode.SUBSCRIBE_ACK.getCode());
        assertThat(mapAckAlice.get("data"))
            .as("Action should echo the Alice's principal")
            .isEqualTo("user-alice");
        assertThat(mapAckBob.get("op")).isEqualTo(TestOpCode.SUBSCRIBE_ACK.getCode());
        assertThat(mapAckBob.get("data"))
            .as("Action should echo the Bob's principal")
            .isEqualTo("user-bob");
        // when
        final String secretMessage = "Hello from Server to Room 51!";
        server.getBroadcaster().broadcast(TestWsTopic.CHAT_ROOM, TestOpCode.BROADCAST_MSG,
            secretMessage);
        // then
        final String aliceReceived = user1Queue.poll(5, TimeUnit.SECONDS);
        final String bobReceived = user2Queue.poll(5, TimeUnit.SECONDS);
        assertThat(aliceReceived).as("Alice should receive the broadcast").isNotNull();
        assertThat(bobReceived).as("Bob should receive the broadcast").isNotNull();
        // for user 1
        final Map<?, ?> mapAlice = jsonSerializer.deserialize(aliceReceived, Map.class);
        assertThat(mapAlice.get("op")).isEqualTo(TestOpCode.BROADCAST_MSG.getCode());
        assertThat(mapAlice.get("data")).isEqualTo(secretMessage);
        // for user 2
        final Map<?, ?> mapBob = jsonSerializer.deserialize(bobReceived, Map.class);
        assertThat(mapBob.get("op")).isEqualTo(TestOpCode.BROADCAST_MSG.getCode());
        assertThat(mapBob.get("data")).isEqualTo(secretMessage);
        // cleanup
        wsUser1.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
        wsUser2.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
    }

    @Test
    @DisplayName("should return UNKNOWN_ACTION when receiving unmapped OpCode")
    void shouldHandleUnknownAction() throws Exception {
        // given
        final LinkedBlockingQueue<String> responses = new LinkedBlockingQueue<>();
        final HttpClient client = HttpClient.newBuilder().build();
        final String url = String.format("ws://localhost:%d/v1?encoding=json&frame=text", port);
        final WebSocket webSocket = client.newWebSocketBuilder()
            .header(CommonHttpHeaderName.AUTHORIZATION.getCode(), TestConstants.SECRET_TOKEN)
            .buildAsync(URI.create(url), new WebSocket.Listener() {
                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    responses.add(data.toString());
                    return null;
                }
            }).join();
        final String badRequest = jsonSerializer.serialize(Map.of("op", 999999, "data", "test"));
        // when
        webSocket.sendText(badRequest, true);
        // then
        final String rawResponse = responses.poll(5, TimeUnit.SECONDS);
        assertThat(rawResponse).isNotNull();
        final Map<?, ?> responseEnvelope = jsonSerializer.deserialize(rawResponse, Map.class);
        assertThat(responseEnvelope.get("op")).isEqualTo(WsOpCode.UNKNOWN_ACTION.getCode());
        // cleanup
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
    }

    @Test
    @DisplayName("should handle malformed JSON and return INVALID_PAYLOAD")
    void shouldHandleMalformedJson() throws Exception {
        // given
        final LinkedBlockingQueue<String> responses = new LinkedBlockingQueue<>();
        final HttpClient client = HttpClient.newBuilder().build();
        final String url = String.format("ws://localhost:%d/v1?encoding=json&frame=text", port);
        final WebSocket webSocket = client.newWebSocketBuilder()
            .header(CommonHttpHeaderName.AUTHORIZATION.getCode(), TestConstants.SECRET_TOKEN)
            .buildAsync(URI.create(url), new WebSocket.Listener() {
                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data,
                                                 boolean last) {
                    responses.add(data.toString());
                    return null;
                }
            }).join();
        // when
        webSocket.sendText("{ bad_json: is here }", true);
        // then
        final String rawResponse = responses.poll(5, TimeUnit.SECONDS);
        assertThat(rawResponse).isNotNull();
        final Map<?, ?> responseEnvelope = jsonSerializer.deserialize(rawResponse, Map.class);
        assertThat(responseEnvelope.get("op")).isEqualTo(WsOpCode.INVALID_PAYLOAD.getCode());
        // cleanup
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
    }

    @Test
    @DisplayName("should send and receive heartbeat via binary frame")
    void shouldHandleBinaryHeartbeatRoundTrip() throws Exception {
        // given
        final LinkedBlockingQueue<byte[]> responses = new LinkedBlockingQueue<>();
        final HttpClient client = HttpClient.newBuilder().build();
        final String url = String.format("ws://localhost:%d/v1?encoding=json&frame=binary", port);
        final WebSocket webSocket = client.newWebSocketBuilder()
            .header(CommonHttpHeaderName.AUTHORIZATION.getCode(), TestConstants.SECRET_TOKEN)
            .buildAsync(URI.create(url), new WebSocket.Listener() {
                @Override
                public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data,
                                                   boolean last) {
                    final byte[] bytes = new byte[data.remaining()];
                    data.get(bytes);
                    responses.add(bytes);
                    webSocket.request(1);
                    return null;
                }
            }).join();
        final String heartbeatRequest = jsonSerializer.serialize(new MessageEnvelope<>(
            WsOpCode.HEARTBEAT.getCode(),
            null
        ));
        final byte[] payloadBytes = heartbeatRequest.getBytes(StandardCharsets.UTF_8);
        // when
        webSocket.sendBinary(java.nio.ByteBuffer.wrap(payloadBytes), true);
        // then
        final byte[] rawResponse = responses.poll(5, TimeUnit.SECONDS);
        assertThat(rawResponse).isNotNull();
        final String responseString = new String(rawResponse, StandardCharsets.UTF_8);
        final Map<?, ?> responseEnvelope = jsonSerializer.deserialize(responseString, Map.class);
        assertThat(responseEnvelope.get("op")).isEqualTo(WsOpCode.HEARTBEAT.getCode());
        // cleanup
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
    }
}
