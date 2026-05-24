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

import java.util.Map;

import xyz.jwizard.jwl.queue.exchange.DefaultExchangeType;
import xyz.jwizard.jwl.queue.exchange.ExchangeType;

public record QueueTopology(
    boolean durable, // whether the queue should survive a broker restart (saved to disk)
    boolean exclusive, // whether the queue is exclusive to this connection
    boolean autoDelete, // whether to delete the queue when the last consumer unsubscribes
    Map<String, Object> arguments, // additional configuration arguments
    String exchangeName, // name of the exchange to bind the queue to (null/empty if none)
    ExchangeType exchangeType, // type of the exchange (most common: "direct", "topic", "fanout")
    String routingKey, // routing key used by the exchange to route messages to this queue
    boolean useDeadLetter // use DLX (dead letter exchange)
) {
    public static Builder builder() {
        return new Builder();
    }

    public boolean hasExchange() {
        return exchangeName != null && !exchangeName.isBlank() && exchangeType != null;
    }

    public static class Builder {
        private boolean durable = true;
        private boolean exclusive = false;
        private boolean autoDelete = false;
        private Map<String, Object> arguments = Map.of();

        private String exchangeName;
        private ExchangeType exchangeType = DefaultExchangeType.DIRECT;
        private String routingKey = "";
        private boolean useDeadLetter = false;

        public Builder durable(boolean durable) {
            this.durable = durable;
            return this;
        }

        public Builder exclusive(boolean exclusive) {
            this.exclusive = exclusive;
            return this;
        }

        public Builder autoDelete(boolean autoDelete) {
            this.autoDelete = autoDelete;
            return this;
        }

        public Builder arguments(Map<String, Object> arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder bindToExchange(String exchangeName, ExchangeType exchangeType,
                                      String routingKey) {
            this.exchangeName = exchangeName;
            this.exchangeType = exchangeType;
            this.routingKey = routingKey;
            return this;
        }

        public Builder withDeadLetter() {
            this.useDeadLetter = true;
            return this;
        }

        public QueueTopology build() {
            return new QueueTopology(
                durable,
                exclusive,
                autoDelete,
                arguments,
                exchangeName,
                exchangeType,
                routingKey,
                useDeadLetter
            );
        }
    }
}
