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
package xyz.noisif.nsl.kv.jedis;

import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.common.util.io.IoUtil;
import xyz.noisif.nsl.kv.KvKey;
import xyz.noisif.nsl.kv.KvServer;
import xyz.noisif.nsl.kv.jedis.factory.ClusterJedisClientFactory;
import xyz.noisif.nsl.kv.jedis.factory.FactoryType;
import xyz.noisif.nsl.kv.jedis.factory.JedisClientFactory;
import xyz.noisif.nsl.kv.jedis.pubsub.JedisPubSubRegistrar;
import xyz.noisif.nsl.kv.pubsub.KvChannel;
import xyz.noisif.nsl.kv.pubsub.PubSubRegistrar;

import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.SetParams;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

public class JedisServer extends KvServer {
  private final JedisClientFactory clientFactory;
  private final int poolMaxTotal;
  private final int poolMaxIdle;
  private final int poolMinIdle;

  private UnifiedJedis redisClient;

  private JedisServer(Builder builder) {
    super(builder);
    clientFactory = builder.factory;
    poolMaxTotal = builder.poolMaxTotal;
    poolMaxIdle = builder.poolMaxIdle;
    poolMinIdle = builder.poolMinIdle;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected final void onKvServerStart() {
    final JedisClientConfig config =
        DefaultJedisClientConfig.builder()
            .password(password != null && !password.isBlank() ? password : null)
            .build();

    final Set<HostAndPort> clusterNodes =
        nodes.stream().map(c -> new HostAndPort(c.host(), c.port())).collect(Collectors.toSet());

    final ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(poolMaxTotal);
    poolConfig.setMinIdle(Math.min(poolMinIdle, poolMaxIdle));
    poolConfig.setMaxIdle(Math.min(poolMaxIdle, poolMaxTotal));

    log.info(
        "KV server pool info: max total: {}, min/max idle: {}/{}",
        poolConfig.getMaxTotal(),
        poolConfig.getMinIdle(),
        poolConfig.getMaxIdle());
    redisClient = clientFactory.create(clusterNodes, config, poolConfig);

    final String pingResponse = redisClient.ping();
    log.debug("KV server ping response: {}", pingResponse);
    log.info("Successfully connected to KV server, mode: {}", clientFactory.type());
  }

  @Override
  protected PubSubRegistrar createRegistrar() {
    return new JedisPubSubRegistrar(redisClient);
  }

  @Override
  protected final void onStop() {
    IoUtil.closeQuietly(redisClient, UnifiedJedis::close);
  }

  @Override
  public void set(KvKey key, String value, Object... keyParams) {
    final String exactKey = key.build(keyParams);
    log.debug("KV SET -> key: '{}'", exactKey);
    redisClient.set(exactKey, value);
  }

  @Override
  public void setWithTtl(KvKey key, String value, Object... keyParams) {
    final String exactKey = key.build(keyParams);
    final long ttl = key.getDefaultTtlSeconds();
    if (ttl > 0) {
      log.debug("KV SETEX -> key: '{}', TTL: {}s", exactKey, ttl);
      redisClient.set(exactKey, value, SetParams.setParams().ex(ttl));
    } else {
      set(key, value, keyParams);
    }
  }

  @Override
  public String get(KvKey key, Object... keyParams) {
    final String exactKey = key.build(keyParams);
    final String value = redisClient.get(exactKey);
    log.debug("KV GET -> key: '{}', found: {}", exactKey, value != null);
    return value;
  }

  @Override
  public void del(KvKey key, Object... keyParams) {
    final String exactKey = key.build(keyParams);
    log.debug("KV DEL -> key: '{}'", exactKey);
    redisClient.del(exactKey);
  }

  @Override
  public void publish(KvChannel channel, String message, Object... channelParams) {
    final String exactChannelName = channel.buildChannel(channelParams);
    log.debug("KV PUBLISH -> channel: '{}'", exactChannelName);
    redisClient.publish(exactChannelName, message);
  }

  @Override
  public void publishBinary(KvChannel channel, byte[] message, Object... channelParams) {
    final String exactChannelName = channel.buildChannel(channelParams);
    final byte[] channelBytes = exactChannelName.getBytes(StandardCharsets.UTF_8);
    log.debug("KV PUBLISH (binary) -> channel: '{}'", exactChannelName);
    redisClient.publish(channelBytes, message);
  }

  public static class Builder extends KvServer.AbstractBuilder<Builder> {
    private int poolMaxTotal = 128;
    private int poolMaxIdle = 64;
    private int poolMinIdle = 16;
    private JedisClientFactory factory = new ClusterJedisClientFactory();

    private Builder() {}

    @Override
    protected Builder self() {
      return this;
    }

    public Builder poolMaxTotal(int poolMaxTotal) {
      this.poolMaxTotal = poolMaxTotal;
      return self();
    }

    public Builder poolMaxIdle(int poolMaxIdle) {
      this.poolMaxIdle = poolMaxIdle;
      return self();
    }

    public Builder poolMinIdle(int poolMinIdle) {
      this.poolMinIdle = poolMinIdle;
      return self();
    }

    public Builder withFactory(FactoryType factoryType) {
      factory = factoryType.getFactory();
      return self();
    }

    public Builder withFactory(JedisClientFactory factory) {
      this.factory = factory;
      return self();
    }

    @Override
    public JedisServer build() {
      validate();
      Assert.notNull(factory, "factory");
      Assert.greaterThan(poolMaxTotal, 0, "poolMaxTotal");
      Assert.greaterOrEqualThan(poolMinIdle, 0, "poolMinIdle");
      Assert.greaterOrEqualThan(poolMaxIdle, 0, "poolMaxIdle");
      Assert.lowerOrEqualThan(poolMinIdle, poolMaxIdle, "poolMinIdle");
      Assert.lowerOrEqualThan(poolMaxIdle, poolMaxTotal, "poolMaxIdle");
      return new JedisServer(this);
    }
  }
}
