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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.ShutdownNotifier;

import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.common.util.thread.TaskExecutor;
import xyz.jwizard.jwl.queue.QueueListener;
import xyz.jwizard.jwl.queue.QueueServer;
import xyz.jwizard.jwl.queue.QueueTopology;
import xyz.jwizard.jwl.queue.rabbitmq.connector.ConnectorType;
import xyz.jwizard.jwl.queue.rabbitmq.connector.RabbitMqClusterConnector;
import xyz.jwizard.jwl.queue.rabbitmq.connector.RabbitMqConnector;

public class RabbitMqServer extends QueueServer {
    // don't send more than 10 messages until finish processing previous
    private static final int BASIC_QOS = 10;
    private static final int NETWORK_RECOVERY_INTERVAL = 5000;

    private final RabbitMqConnector connector;
    private final String virtualHost;

    private Connection connection;
    private Channel channel;
    private TaskExecutor taskExecutor;

    private RabbitMqServer(Builder builder) {
        super(builder);
        connector = builder.connector;
        virtualHost = builder.virtualHost;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected final void onQueueServerStart() throws Exception {
        if (connection != null) {
            return;
        }
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        // virtual threads
        taskExecutor = TaskExecutor.createDefault("rabbitmq-worker");
        factory.setThreadFactory(Thread.ofVirtual().name("rabbit-vt-", 1).factory());
        factory.setSharedExecutor(taskExecutor.getDelegate());

        factory.setAutomaticRecoveryEnabled(true);
        // recovers topology (queues, bindings) after node restarts
        factory.setTopologyRecoveryEnabled(true);
        // 5-second interval between reconnection attempts
        factory.setNetworkRecoveryInterval(NETWORK_RECOVERY_INTERVAL);

        connection = connector.connect(nodes, factory);
        channel = connection.createChannel();
        log.info("Successfully connected to queue server, mode: {}", connector.type());
    }

    @Override
    protected final void onStop() {
        IoUtil.closeQuietly(channel, ShutdownNotifier::isOpen, Channel::close);
        IoUtil.closeQuietly(connection, ShutdownNotifier::isOpen, Connection::close);
        IoUtil.closeQuietly(taskExecutor);
    }

    @Override
    protected void onPublish(String exchange, String routingKey, byte[] body) throws Exception {
        if (channel == null || !channel.isOpen()) {
            throw new IllegalStateException("Cannot publish message, " +
                "channel is closed or not initialized");
        }
        channel.basicPublish(exchange, routingKey, MessageProperties.PERSISTENT_BASIC, body);
    }

    @Override
    protected void onRegisterListener(QueueListener<?> listener) throws IOException {
        final String queueName = listener.getQueueName();
        final QueueTopology topology = listener.getTopology();

        final Map<String, Object> args = new HashMap<>(topology.arguments());
        if (topology.useDeadLetter()) {
            final String dlx = queueName + ".dlx";
            final String dlq = queueName + ".dlq";
            final String rk = "dead";

            channel.exchangeDeclare(dlx, BuiltinExchangeType.DIRECT, true);
            channel.queueDeclare(dlq, true, false, false, null);
            channel.queueBind(dlq, dlx, rk);
            args.put("x-dead-letter-exchange", dlx);
            args.put("x-dead-letter-routing-key", rk);

            log.info("Auto-configured DLX for '{}' -> messages will end up in '{}'", queueName,
                dlq);
        }
        channel.queueDeclare(
            queueName,
            topology.durable(),
            topology.exclusive(),
            topology.autoDelete(),
            args
        );

        channel.basicQos(BASIC_QOS);
        if (topology.hasExchange()) {
            final String exchangeName = topology.exchangeName();
            final String routingKey = topology.routingKey();
            channel.exchangeDeclare(exchangeName, topology.exchangeType().getType(), true);
            channel.queueBind(queueName, exchangeName, routingKey);
            log.info("Bound queue '{}' to exchange '{}' with routing key '{}'",
                queueName, exchangeName, routingKey);
        }
        channel.basicConsume(queueName, false, (consumerTag, message) -> {
            final long deliveryTag = message.getEnvelope().getDeliveryTag();
            try {
                processDelivery(listener, message.getBody());
                channel.basicAck(deliveryTag, false);
            } catch (Throwable t) {
                log.error("Processing failed for message on {}, sending to DLX", queueName, t);
                try {
                    // requeue set to false, without DLX delete message
                    channel.basicNack(deliveryTag, false, false);
                } catch (IOException e) {
                    log.error("Critical: could not send NACK", e);
                }
            }
        }, cancelTag -> {
        });
    }

    public static class Builder extends QueueServer.AbstractBuilder<Builder> {
        private RabbitMqConnector connector = new RabbitMqClusterConnector();
        private String virtualHost;

        private Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder withConnector(ConnectorType connectorType) {
            connector = connectorType.getConnector();
            return this;
        }

        public Builder withConnector(RabbitMqConnector connector) {
            this.connector = connector;
            return this;
        }

        public Builder virtualHost(String virtualHost) {
            this.virtualHost = virtualHost;
            return this;
        }

        @Override
        public QueueServer build() {
            validate();
            virtualHost = Objects.requireNonNullElse(virtualHost, "/");
            return new RabbitMqServer(this);
        }
    }
}
