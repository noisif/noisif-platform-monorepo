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
package xyz.jwizard.jwl.queue.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;

import xyz.jwizard.jwl.codec.serialization.MessageSerializer;
import xyz.jwizard.jwl.codec.serialization.SerializerRegistry;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;
import xyz.jwizard.jwl.codec.serialization.json.JacksonSerializer;
import xyz.jwizard.jwl.codec.serialization.raw.RawByteSerializer;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.reflect.TypeReference;
import xyz.jwizard.jwl.common.util.CastUtil;
import xyz.jwizard.jwl.net.HostPort;
import xyz.jwizard.jwl.queue.FailingListener;
import xyz.jwizard.jwl.queue.HappyPathListener;
import xyz.jwizard.jwl.queue.JsonCommandListener;
import xyz.jwizard.jwl.queue.MessagePublisher;
import xyz.jwizard.jwl.queue.PlayTrackCommand;
import xyz.jwizard.jwl.queue.QueueListener;
import xyz.jwizard.jwl.queue.QueueServer;
import xyz.jwizard.jwl.queue.rabbitmq.connector.ConnectorType;

@Testcontainers
@ExtendWith(MockitoExtension.class)
public class RabbitMqServerIntegrationTest {
    @Container
    static final RabbitMQContainer rabbitMQ = new RabbitMQContainer(
        DockerImageName.parse("rabbitmq:3-management")
    );

    private QueueServer server;
    private MessagePublisher messagePublisher;
    @Mock
    private ComponentProvider componentProvider;
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
        final String receivedStr = new String(listener.getReceivedMessage(),
            StandardCharsets.UTF_8);
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
        final PlayTrackCommand command = new PlayTrackCommand("123456789",
            "https://youtube.com/watch?v=123");
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
        server = RabbitMqServer.builder()
            .withConnector(ConnectorType.SINGLE_NODE)
            .nodes(Set.of(HostPort.from(rabbitMQ.getHost(), rabbitMQ.getAmqpPort())))
            .username(rabbitMQ.getAdminUsername())
            .password(rabbitMQ.getAdminPassword())
            .virtualHost("/")
            .componentProvider(componentProvider)
            .serializerRegistry(serializerRegistry)
            .build();
        messagePublisher = server.getQueuePublisher();
        server.start();
    }

    private Connection createDirectConnection() throws Exception {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMQ.getHost());
        factory.setPort(rabbitMQ.getAmqpPort());
        factory.setUsername(rabbitMQ.getAdminUsername());
        factory.setPassword(rabbitMQ.getAdminPassword());
        return factory.newConnection();
    }

    private void mockListenerRegistration(QueueListener<?> listener) {
        when(componentProvider.getInstancesOf(CastUtil
                .<TypeReference<QueueListener<?>>>unsafeCast(any(TypeReference.class))
            )
        ).thenReturn(Collections.singletonList(listener));
    }
}
