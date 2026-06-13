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
package xyz.noisif.nsl.kv.jedis.factory;

import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.UnifiedJedis;

import java.util.Set;

public class SingleNodeJedisClientFactory implements JedisClientFactory {
  @Override
  public UnifiedJedis create(
      Set<HostAndPort> nodes, JedisClientConfig config, ConnectionPoolConfig poolConfig) {
    final HostAndPort singleNode = nodes.iterator().next();
    return RedisClient.builder()
        .hostAndPort(singleNode.getHost(), singleNode.getPort())
        .poolConfig(poolConfig)
        .build();
  }

  @Override
  public FactoryType type() {
    return FactoryType.SINGLE_NODE;
  }
}
