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
package xyz.jwizard.jwl.netclient.websocket.jetty;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializerRegistry;
import xyz.jwizard.jwl.codec.envelope.json.JsonBinaryEnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.protobuf.ProtobufEnvelopeSerializer;
import xyz.jwizard.jwl.codec.serialization.json.JsonSerializer;
import xyz.jwizard.jwl.codec.serialization.protobuf.ProtobufSerializer;
import xyz.jwizard.jwl.common.di.ApplicationContext;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.di.GuiceComponentProvider;
import xyz.jwizard.jwl.common.reflect.ClassGraphScanner;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.net.envelope.ActionGroup;
import xyz.jwizard.jwl.netclient.TestConstants;
import xyz.jwizard.jwl.netclient.websocket.GenericWsClient;
import xyz.jwizard.jwl.netclient.websocket.TestQueueProvider;
import xyz.jwizard.jwl.netclient.websocket.TestWsClientGroup;
import xyz.jwizard.jwl.netclient.websocket.TestWsOpCode;
import xyz.jwizard.jwl.netclient.websocket.group.WsClientGroupConfig;
import xyz.jwizard.jwl.netclient.websocket.group.WsReconnectConfig;
import xyz.jwizard.jwl.netclient.websocket.listener.WsClientEnvelopeBusListener;
import xyz.jwizard.jwl.netclient.websocket.protobuf.TestPayloadProto;
import xyz.jwizard.jwl.websocket.WsServer;
import xyz.jwizard.jwl.websocket.auth.WsTokenAuthenticator;
import xyz.jwizard.jwl.websocket.dispatcher.ConcurrentLocalSessionDispatcher;
import xyz.jwizard.jwl.websocket.jetty.JettyWsServer;
import xyz.jwizard.jwl.websocket.listener.ActionRouterWsMessageListener;
import xyz.jwizard.jwl.websocket.negotation.QueryParamSerializerResolver;
import xyz.jwizard.jwl.websocket.registry.InMemoryWsSessionRegistry;
import xyz.jwizard.jwl.websocket.registry.WsSubscriptionRegistry;

class JettyWsEnvelopeIntegrationTest {
    private final JsonSerializer jsonSerializer = TestConstants.JSON_SERIALIZER;
    private final TestQueueProvider testQueueProvider = new TestQueueProvider();

    private ClassScanner scanner;
    private ProtobufSerializer protobufSerializer;
    private InMemoryWsSessionRegistry registry;
    private ComponentProvider componentProvider;
    private WsServer server;
    private GenericWsClient client;

    @BeforeEach
    void setUp() {
        scanner = new ClassGraphScanner(
            "xyz.jwizard.jwl.netclient.websocket",
            "xyz.jwizard.jwl.codec.envelope.protobuf" // for protobuf
        );
        protobufSerializer = ProtobufSerializer.createDefault(scanner);
        registry = InMemoryWsSessionRegistry.createDefault();
        final ApplicationContext context = ApplicationContext.create(scanner, Map.of(
            ComponentProvider.class, GuiceComponentProvider.class
        ), Map.of(
            TestQueueProvider.class, testQueueProvider,
            WsSubscriptionRegistry.class, registry
        ));
        componentProvider = context.getComponentProvider();
        // setup server
        server = createWsServer(0);
        server.start();
        // setup client
        client = createWsClient(server.getLocalPort());
        client.start();
    }

    private WsServer createWsServer(int port) {
        return JettyWsServer.builder()
            .port(port)
            .path("/v1")
            .componentProvider(componentProvider)
            .serializerRegistry(EnvelopeSerializerRegistry.createEnvelopeRegistry()
                .registerJsonDefaults(jsonSerializer)
                .registerProtobufDefaults(protobufSerializer)
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
            .addBusListener(ActionRouterWsMessageListener.builder()
                .actionGroup(ActionGroup.GLOBAL)
                .componentProvider(componentProvider)
                .build()
            )
            .build();
    }

    private GenericWsClient createWsClient(int port) {
        return JettyWsClient.builder()
            .defaultClientGroup(WsClientGroupConfig.builder()
                .url("ws://localhost:" + port + "/v1?auth_token=" +
                    TestConstants.SECRET_TOKEN)
                .principalId(TestConstants.SERVICE_NAME)
                .componentProvider(componentProvider)
                .reconnectConfig(WsReconnectConfig.enabled(Duration.ofSeconds(1), 4))
                .setEnvelopeMode()
                .envelopeBusConfig(config -> config
                    .encodingParamName("encoding")
                    .dataTypeParamName("frame")
                    .serializer(JsonBinaryEnvelopeSerializer.createDefault(jsonSerializer))
                    .addBusListener(WsClientEnvelopeBusListener.builder()
                        .actionGroup(ActionGroup.GLOBAL)
                        .componentProvider(componentProvider)
                        .build()
                    )
                )
                .build()
            )
            .clientGroup(TestWsClientGroup.PROTOBUF, WsClientGroupConfig.builder()
                .url("ws://localhost:" + port + "/v1?auth_token=" +
                    TestConstants.SECRET_TOKEN)
                .principalId(TestConstants.SERVICE_NAME)
                .componentProvider(componentProvider)
                .setEnvelopeMode()
                .envelopeBusConfig(config -> config
                    .encodingParamName("encoding")
                    .dataTypeParamName("frame")
                    .serializer(ProtobufEnvelopeSerializer.createDefault(protobufSerializer))
                    .addBusListener(WsClientEnvelopeBusListener.builder()
                        .actionGroup(ActionGroup.GLOBAL)
                        .componentProvider(componentProvider)
                        .build()
                    )
                )
                .build()
            )
            .build();
    }

    @AfterEach
    void tearDown() {
        IoUtil.closeQuietly(client);
        IoUtil.closeQuietly(server);
        IoUtil.closeQuietly(scanner);
        testQueueProvider.clear();
    }

    @Test
    @DisplayName("should perform send and receive exchange via envelopes")
    void shouldPerformSendAndReceiveExchangeViaEnvelopes() throws Exception {
        // given
        final String testMessage = "Hello WebSocket";
        // when
        client.sendEnvelope(TestWsOpCode.SEND_DATA, testMessage);
        // then
        final Object response = testQueueProvider.get().poll(5, SECONDS);
        final String expected = "Send: " + testMessage + " to: " + TestConstants.SERVICE_NAME;
        assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("should send and receive protobuf payload natively")
    void shouldSendAndReceiveProtobufPayload() {
        // given
        final String message = "hello";
        final TestPayloadProto.MyMessage requestMsg = TestPayloadProto.MyMessage.newBuilder()
            .setId(404)
            .setContent(message)
            .build();
        // when
        client.sendEnvelope(TestWsClientGroup.PROTOBUF, TestWsOpCode.SEND_DATA_PROTO, requestMsg);
        await().atMost(5, SECONDS).until(() -> !testQueueProvider.get().isEmpty());
        // then
        final Object receivedObject = testQueueProvider.get().poll();
        assertThat(receivedObject).isNotNull();
        assertThat(receivedObject).isInstanceOf(TestPayloadProto.MyMessage.class);
        final TestPayloadProto.MyMessage responseMsg = (TestPayloadProto.MyMessage) receivedObject;
        assertThat(responseMsg.getId()).isEqualTo(404);
        assertThat(responseMsg.getContent()).isEqualTo("Received: " + message);
    }

    @Test
    @DisplayName("should reconnect after server failure")
    void shouldReconnectAfterServerFailure() {
        // given
        assertThat(client.isConnected()).isTrue();
        final int originalPort = server.getLocalPort();
        server.close();
        // when
        await().atMost(5, SECONDS).until(() -> !client.isConnected());
        // then
        server = createWsServer(originalPort);
        server.start();
        await().atMost(15, SECONDS).until(() -> client.isConnected());
        assertThat(client.isConnected()).isTrue();
    }
}
