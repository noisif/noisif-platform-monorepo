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
package xyz.noisif.nsl.queue.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import xyz.noisif.nsl.codec.serialization.MessageSerializer;
import xyz.noisif.nsl.codec.serialization.SerializerRegistry;
import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;
import xyz.noisif.nsl.codec.serialization.json.JacksonSerializer;
import xyz.noisif.nsl.codec.serialization.raw.RawByteSerializer;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.reflect.TypeReference;
import xyz.noisif.nsl.common.util.CastUtil;
import xyz.noisif.nsl.net.HostPort;
import xyz.noisif.nsl.queue.FailingListener;
import xyz.noisif.nsl.queue.HappyPathListener;
import xyz.noisif.nsl.queue.JsonCommandListener;
import xyz.noisif.nsl.queue.MessagePublisher;
import xyz.noisif.nsl.queue.PlayTrackCommand;
import xyz.noisif.nsl.queue.QueueListener;
import xyz.noisif.nsl.queue.QueueServer;
import xyz.noisif.nsl.queue.rabbitmq.connector.ConnectorType;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class RabbitMqServerIntegrationTest {
  @Container
  static final RabbitMQContainer rabbitMq =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

  private QueueServer server;
  private MessagePublisher messagePublisher;
  @Mock private ComponentProvider componentProvider;
  private SerializerRegistry<MessageSerializer> serializerRegistry;

  @BeforeEach
  void setUp() {
    serializerRegistry = SerializerRegistry.create();
    serializerRegistry.register(RawByteSerializer.createDefault());
    serializerRegistry.register(JacksonSerializer.createLenientForMessaging());
  }

  @AfterEach
  void tearDown() {
    server.close();
  }

  @Test
  @DisplayName("should publish and receive raw byte message successfully")
  void shouldPublishAndReceiveMessage() throws InterruptedException {
    // given
    final HappyPathListener listener = new HappyPathListener();
    mockListenerRegistration(listener);
    startServer();
    // when
    final byte[] payload = "Hello RabbitMQ".getBytes(StandardCharsets.UTF_8);
    messagePublisher.publishToQueue("test.happy.queue", payload, StandardSerializerFormat.RAW);

    // then
    final boolean received = listener.getLatch().await(5, TimeUnit.SECONDS);
    assertThat(received).as("Message should be received").isTrue();
    final String receivedStr = new String(listener.getReceivedMessage(), StandardCharsets.UTF_8);
    assertThat(receivedStr).isEqualTo("Hello RabbitMQ");
  }

  @Test
  @DisplayName("should route failed messages to Dead Letter Queue when DLX is enabled")
  void shouldRouteToDlxOnFailure() throws Exception {
    // given
    final FailingListener listener = new FailingListener();
    mockListenerRegistration(listener);
    startServer();
    // when
    final byte[] poisonPill = "Poison Pill".getBytes(StandardCharsets.UTF_8);
    messagePublisher.publishToQueue("test.fail.queue", poisonPill, StandardSerializerFormat.RAW);
    // then
    Thread.sleep(500);
    try (final Connection conn = createDirectConnection();
        Channel channel = conn.createChannel()) {

      final GetResponse response = channel.basicGet("test.fail.queue.dlq", true);
      assertThat(response).as("Message should be routed to DLQ").isNotNull();

      final String dlqMessage = new String(response.getBody(), StandardCharsets.UTF_8);
      assertThat(dlqMessage).isEqualTo("Poison Pill");
    }
  }

  @Test
  @DisplayName("should serialize, publish, receive and deserialize JSON object successfully")
  void shouldPublishAndReceiveJsonObject() throws InterruptedException {
    // given
    final JsonCommandListener listener = new JsonCommandListener();
    mockListenerRegistration(listener);
    startServer();
    final PlayTrackCommand command =
        new PlayTrackCommand("123456789", "https://youtube.com/watch?v=123");
    // when
    messagePublisher.publishToQueue("test.json.queue", command);
    // then
    final boolean received = listener.getLatch().await(5, TimeUnit.SECONDS);
    assertThat(received).as("JSON message should be received").isTrue();
    assertThat(listener.getReceivedCommand()).isNotNull();
    assertThat(listener.getReceivedCommand().guildId()).isEqualTo("123456789");
    assertThat(listener.getReceivedCommand().trackUrl())
        .isEqualTo("https://youtube.com/watch?v=123");
  }

  private void startServer() {
    server =
        RabbitMqServer.builder()
            .withConnector(ConnectorType.SINGLE_NODE)
            .nodes(Set.of(HostPort.from(rabbitMq.getHost(), rabbitMq.getAmqpPort())))
            .username(rabbitMq.getAdminUsername())
            .password(rabbitMq.getAdminPassword())
            .virtualHost("/")
            .componentProvider(componentProvider)
            .serializerRegistry(serializerRegistry)
            .build();
    messagePublisher = server.getQueuePublisher();
    server.start();
  }

  private Connection createDirectConnection() throws Exception {
    final ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitMq.getHost());
    factory.setPort(rabbitMq.getAmqpPort());
    factory.setUsername(rabbitMq.getAdminUsername());
    factory.setPassword(rabbitMq.getAdminPassword());
    return factory.newConnection();
  }

  private void mockListenerRegistration(QueueListener<?> listener) {
    when(componentProvider.getInstancesOf(
            CastUtil.<TypeReference<QueueListener<?>>>unsafeCast(any(TypeReference.class))))
        .thenReturn(Collections.singletonList(listener));
  }
}
