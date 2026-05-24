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
package xyz.jwizard.jwl.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.serialization.SerializerFormat;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;

public class QueuePublisher implements MessagePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(QueuePublisher.class);

    private final QueueServer queueServer;

    QueuePublisher(QueueServer queueServer) {
        this.queueServer = queueServer;
    }

    @Override
    public <T> void publish(String exchange, String routingKey, T payload) {
        publish(exchange, routingKey, payload, StandardSerializerFormat.JSON);
    }

    @Override
    public <T> void publish(String exchange, String routingKey, T payload,
                            SerializerFormat format) {
        final String logExchange = (exchange == null || exchange.isBlank())
            ? "<default>"
            : exchange;
        try {
            LOG.trace("Publishing message to exchange '{}' with routing key '{}'", logExchange,
                routingKey);
            final byte[] body = queueServer.getSerializerRegistry()
                .get(format)
                .serializeToBytes(payload);
            queueServer.onPublish(exchange, routingKey, body);
        } catch (Exception ex) {
            LOG.error("Failed to publish message to exchange '{}' with routing key '{}'",
                logExchange, routingKey, ex);
        }
    }

    @Override
    public <T> void publishToQueue(String queueName, T payload) {
        publish("", queueName, payload);
    }

    @Override
    public <T> void publishToQueue(String queueName, T payload, SerializerFormat format) {
        publish("", queueName, payload, format);
    }
}
