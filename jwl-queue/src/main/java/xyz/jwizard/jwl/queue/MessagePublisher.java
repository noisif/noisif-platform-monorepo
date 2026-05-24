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

import xyz.jwizard.jwl.codec.serialization.SerializerFormat;

public interface MessagePublisher {
    <T> void publish(String exchange, String routingKey, T payload);

    <T> void publish(String exchange, String routingKey, T payload, SerializerFormat format);

    <T> void publishToQueue(String queueName, T payload);

    <T> void publishToQueue(String queueName, T payload, SerializerFormat format);
}
