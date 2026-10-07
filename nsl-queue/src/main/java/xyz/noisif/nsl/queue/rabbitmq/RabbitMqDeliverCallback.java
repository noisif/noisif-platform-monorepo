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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import xyz.noisif.nsl.queue.MessageAcknowledgment;
import xyz.noisif.nsl.queue.MessageHandler;
import xyz.noisif.nsl.queue.MessageProcessor;
import xyz.noisif.nsl.queue.QueueListener;

class RabbitMqDeliverCallback implements DeliverCallback {
  private final MessageProcessor messageProcessor;
  private final MessageHandler messageHandler;
  private final QueueListener<?> listener;
  private final Channel channel;

  RabbitMqDeliverCallback(
      MessageProcessor messageProcessor,
      MessageHandler messageHandler,
      QueueListener<?> listener,
      Channel channel) {
    this.messageProcessor = messageProcessor;
    this.messageHandler = messageHandler;
    this.listener = listener;
    this.channel = channel;
  }

  @Override
  public void handle(String consumerTag, Delivery message) {
    final long deliveryTag = message.getEnvelope().getDeliveryTag();
    final MessageAcknowledgment ack = new RabbitMqMessageAcknowledgment(channel, deliveryTag);
    messageProcessor.process(
        () -> messageHandler.processDelivery(listener, message.getBody()), ack);
  }
}
