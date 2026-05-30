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

import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.serialization.raw.RawByteSerializer;
import xyz.jwizard.jwl.codec.serialization.raw.RawTextSerializer;
import xyz.jwizard.jwl.common.di.ApplicationContext;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.di.GuiceComponentProvider;
import xyz.jwizard.jwl.common.reflect.ClassGraphScanner;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.netclient.TestConstants;
import xyz.jwizard.jwl.netclient.websocket.GenericWsClient;
import xyz.jwizard.jwl.netclient.websocket.TestQueueProvider;
import xyz.jwizard.jwl.netclient.websocket.TestWsClientGroup;
import xyz.jwizard.jwl.netclient.websocket.WebsocketServerIntegrationMock;
import xyz.jwizard.jwl.netclient.websocket.bus.RawByteBusListener;
import xyz.jwizard.jwl.netclient.websocket.bus.RawTextBusListener;
import xyz.jwizard.jwl.netclient.websocket.group.WsClientGroupConfig;
import xyz.jwizard.jwl.netclient.websocket.group.WsReconnectConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Map;

class JettyWsRawIntegrationTest {
  private final TestQueueProvider testQueueProvider = new TestQueueProvider();

  private ClassScanner scanner;
  private ComponentProvider componentProvider;
  private WebsocketServerIntegrationMock webSocketServer;
  private GenericWsClient client;

  @BeforeEach
  void setUp() throws IOException {
    scanner = new ClassGraphScanner("xyz.jwizard.jwl.netclient.websocket");
    final ApplicationContext context =
        ApplicationContext.create(
            scanner,
            Map.of(ComponentProvider.class, GuiceComponentProvider.class),
            Map.of(TestQueueProvider.class, testQueueProvider));
    componentProvider = context.getComponentProvider();
    // setup server
    webSocketServer = createWsServer(getRandomPort());
    webSocketServer.start();
    // setup client
    client = createWsClient(webSocketServer.getPort());
    client.start();
  }

  private WebsocketServerIntegrationMock createWsServer(int port) {
    return new WebsocketServerIntegrationMock(port);
  }

  private GenericWsClient createWsClient(int port) {
    return JettyWsClient.builder()
        .defaultClientGroup(
            WsClientGroupConfig.builder()
                .url("ws://localhost:" + port)
                .principalId(TestConstants.SERVICE_NAME)
                .componentProvider(componentProvider)
                .reconnectConfig(WsReconnectConfig.enabled(Duration.ofSeconds(2), 5))
                .setTypedMessageMode()
                .typedMessageBusConfig(
                    config ->
                        config
                            .dataTypeParamName(TestConstants.DATA_TYPE_QUERY_PARAM_NAME)
                            .serializer(RawTextSerializer.createDefault())
                            .addBusListener(new RawTextBusListener(testQueueProvider)))
                .build())
        .clientGroup(
            TestWsClientGroup.RAW_BYTE,
            WsClientGroupConfig.builder()
                .url("ws://localhost:" + port)
                .principalId(TestConstants.SERVICE_NAME)
                .componentProvider(componentProvider)
                .reconnectConfig(WsReconnectConfig.enabled(Duration.ofSeconds(2), 5))
                .setTypedMessageMode()
                .typedMessageBusConfig(
                    config ->
                        config
                            .dataTypeParamName(TestConstants.DATA_TYPE_QUERY_PARAM_NAME)
                            .serializer(RawByteSerializer.createDefault())
                            .addBusListener(new RawByteBusListener(testQueueProvider)))
                .build())
        .build();
  }

  private int getRandomPort() throws IOException {
    int port;
    try (final ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    return port;
  }

  @AfterEach
  void tearDown() {
    IoUtil.closeQuietly(client);
    IoUtil.closeQuietly(webSocketServer, WebSocketServer::stop);
    IoUtil.closeQuietly(scanner);
    testQueueProvider.clear();
  }

  @Test
  @DisplayName("should send and receive raw bytes payload")
  void shouldSendAndReceiveRawBytesPayload() {
    // given
    final byte[] originalBytes = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05};
    // when
    final WebSocket session = webSocketServer.getSession(DataType.BINARY);
    session.send(originalBytes);
    await().atMost(5, SECONDS).until(() -> !testQueueProvider.get().isEmpty());
    // then
    final byte[] receivedBytes = (byte[]) testQueueProvider.get().poll();
    assertThat(receivedBytes).isNotNull().containsExactly(originalBytes);
  }

  @Test
  @DisplayName("should send and receive raw text payload")
  void shouldSendAndReceiveRawTextPayload() {
    // given
    final String originalText = "hello ws";
    // when
    final WebSocket session = webSocketServer.getSession(DataType.TEXT);
    session.send(originalText);
    await().atMost(5, SECONDS).until(() -> !testQueueProvider.get().isEmpty());
    // then
    final String receivedText = (String) testQueueProvider.get().poll();
    assertThat(receivedText).isNotNull().contains(receivedText);
  }

  @Test
  @DisplayName("should reconnect after server failure")
  void shouldReconnectAfterServerFailure() throws InterruptedException {
    // given
    assertThat(client.isConnected()).isTrue();
    final int originalPort = webSocketServer.getPort();
    webSocketServer.stop();
    // when
    await().atMost(5, SECONDS).until(() -> !client.isConnected());
    // then
    webSocketServer = createWsServer(originalPort);
    webSocketServer.start();
    await().atMost(15, SECONDS).until(() -> client.isConnected());
    assertThat(client.isConnected()).isTrue();
  }
}
