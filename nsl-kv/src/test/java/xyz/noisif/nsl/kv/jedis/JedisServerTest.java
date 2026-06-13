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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.kv.TestKvKey;
import xyz.noisif.nsl.kv.jedis.factory.JedisClientFactory;
import xyz.noisif.nsl.net.HostPort;

import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.params.SetParams;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
class JedisServerTest {
  @Mock private JedisClientFactory factory;
  @Mock private RedisClusterClient redisClient;
  @Mock private ComponentProvider componentProvider;

  private JedisServer jedisServer;

  @BeforeEach
  void setup() {
    // given
    Mockito.when(factory.create(any(), any(), any())).thenReturn(redisClient);
    jedisServer =
        JedisServer.builder()
            .nodes(Set.of(HostPort.from("localhost", 6379)))
            .withFactory(factory)
            .componentProvider(componentProvider)
            .build();
    jedisServer.start();
  }

  @Test
  @DisplayName("should correctly format key using enum and delegate to underlying client")
  void shouldCorrectlyBuildKeyFromEnumAndCallSet() {
    // given
    final String value = "NOISIF";
    final int param = 123;
    final TestKvKey key = TestKvKey.USER_PROFILE;
    // when
    jedisServer.set(key, value, param);
    // then
    verify(redisClient).set(TestKvKey.USER_PROFILE.build(param), value);
  }

  @Test
  @DisplayName("should append SetParams with EX flag when saving key with Time-To-Live")
  void shouldUseSetParamsWhenEnumHasTtl() {
    // given
    final String value = "some-value";
    final TestKvKey key = TestKvKey.TEMP_SESSION;
    // when
    jedisServer.setWithTtl(key, value);
    // then
    verify(redisClient).set(eq(TestKvKey.TEMP_SESSION.build()), eq(value), any(SetParams.class));
  }
}
