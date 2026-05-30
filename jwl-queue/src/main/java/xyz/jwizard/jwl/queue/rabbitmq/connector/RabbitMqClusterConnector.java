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
package xyz.jwizard.jwl.queue.rabbitmq.connector;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import xyz.jwizard.jwl.net.HostPort;

import java.util.Set;

public class RabbitMqClusterConnector implements RabbitMqConnector {
  @Override
  public Connection connect(Set<HostPort> nodes, ConnectionFactory baseFactory) throws Exception {
    final Address[] addresses =
        nodes.stream().map(node -> new Address(node.host(), node.port())).toArray(Address[]::new);
    return baseFactory.newConnection(addresses);
  }

  @Override
  public ConnectorType type() {
    return ConnectorType.CLUSTER;
  }
}
