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
package xyz.jwizard.jwl.kv.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

import xyz.jwizard.jwl.common.di.ApplicationContext;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.di.GuiceComponentProvider;
import xyz.jwizard.jwl.common.reflect.ClassGraphScanner;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.kv.KvKey;
import xyz.jwizard.jwl.kv.TestKvKey;
import xyz.jwizard.jwl.kv.jedis.factory.FactoryType;
import xyz.jwizard.jwl.kv.jedis.pubsub.ParameterizedStringTestSubscriber;
import xyz.jwizard.jwl.kv.jedis.pubsub.PatternStringTestSubscriber;
import xyz.jwizard.jwl.kv.jedis.pubsub.SimpleBinaryTestSubscriber;
import xyz.jwizard.jwl.kv.jedis.pubsub.SimpleStringTestSubscriber;
import xyz.jwizard.jwl.kv.pubsub.KvChannel;
import xyz.jwizard.jwl.kv.pubsub.TestKvChannel;
import xyz.jwizard.jwl.net.HostPort;

@Testcontainers
class JedisServerIntegrationTest {
    private static final int REDIS_PORT = 6379;

    @Container
    static RedisContainer redisContainer = new RedisContainer(
        DockerImageName.parse("redis:7.2-alpine")
    ).withExposedPorts(REDIS_PORT);

    private static JedisServer jedisServer;
    private static ComponentProvider componentProvider;
    private static ClassScanner scanner;

    @BeforeAll
    static void setupAll() {
        scanner = new ClassGraphScanner("xyz.jwizard.jwl.kv");
        final ApplicationContext context = ApplicationContext.createDefault(scanner, Map.of(
            ComponentProvider.class, GuiceComponentProvider.class
        ));
        final String host = redisContainer.getHost();
        final int port = redisContainer.getMappedPort(REDIS_PORT);
        componentProvider = context.getComponentProvider();
        jedisServer = JedisServer.builder()
            .nodes(Set.of(HostPort.from(host, port)))
            .poolMaxTotal(128)
            .poolMinIdle(16)
            .poolMaxIdle(64)
            .withFactory(FactoryType.SINGLE_NODE)
            .componentProvider(componentProvider)
            .build();
        jedisServer.start();
        jedisServer.awaitSubscribers(3000);
    }

    @AfterAll
    static void tearDownAll() {
        jedisServer.close();
        IoUtil.closeQuietly(scanner);
    }

    @Test
    @DisplayName("should successfully save and retrieve data from real Redis instance")
    void shouldPerformRealSetAndGetOperations() {
        // given
        final KvKey key = TestKvKey.USER_PROFILE;
        final String userId = "999";
        final String expectedValue = "JWizard_Admin";
        // when
        jedisServer.set(key, expectedValue, userId);
        final String actualValue = jedisServer.get(key, userId);
        // then
        assertEquals(expectedValue, actualValue,
            "Value retrieved from Redis should match the one we set");
    }

    @Test
    @DisplayName("should automatically remove key after specified TTL expires")
    void shouldHandleExpirationWithTtl() throws InterruptedException {
        // given
        final KvKey key = TestKvKey.TEMP_SESSION;
        final long ttl = TestKvKey.TEMP_SESSION.getDefaultTtlSeconds();
        // when
        jedisServer.setWithTtl(key, "temporary-data");
        // then
        assertEquals("temporary-data", jedisServer.get(key));
        Thread.sleep((ttl * 2) * 1000 + ttl);
        assertNull(jedisServer.get(key), "Key should have expired and been removed");
    }

    @Test
    @DisplayName("should successfully delete a specific key from Redis")
    void shouldDeleteExistingKeySuccessfully() {
        // given
        final KvKey key = TestKvKey.USER_PROFILE;
        final String userId = "delete_user_777";
        final String expectedValue = "Data_to_be_deleted";
        jedisServer.set(key, expectedValue, userId);
        final String valueBeforeDeletion = jedisServer.get(key, userId);
        assertNotNull(valueBeforeDeletion, "Key should exist before deletion");
        assertEquals(expectedValue, valueBeforeDeletion,
            "Retrieved value should match what was set");
        // when
        jedisServer.del(key, userId);
        // then
        final String valueAfterDeletion = jedisServer.get(key, userId);
        assertNull(valueAfterDeletion, "Key should return null after being deleted");
    }

    @Test
    @DisplayName("should auto-discover, register and receive message via DI pub/sub channel")
    void shouldPublishAndReceiveMessage() throws InterruptedException {
        // given
        final KvChannel channel = TestKvChannel.TEST_EVENTS;
        final String expectedMessage = "Hello_PubSub";
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> receivedRef = new AtomicReference<>();
        final SimpleStringTestSubscriber subscriber = componentProvider
            .getInstance(SimpleStringTestSubscriber.class);
        subscriber.prepareForTest(latch, receivedRef);
        // when
        jedisServer.publish(channel, expectedMessage);
        final boolean messageArrived = latch.await(3, TimeUnit.SECONDS);
        assertTrue(messageArrived, "Did not receive pub/sub message within timeout");
        assertEquals(expectedMessage, receivedRef.get(),
            "Received message does not match published message");
    }

    @Test
    @DisplayName("should auto-discover, register and receive params message via DI pub/sub channel")
    void shouldPublishAndReceiveParameterizedMessage() throws InterruptedException {
        // given
        final KvChannel channel = TestKvChannel.USER_NOTIFICATIONS;
        final String userId = ParameterizedStringTestSubscriber.TEST_USER_ID;
        final String expectedMessage = "You have a new alert";
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> receivedRef = new AtomicReference<>();
        final ParameterizedStringTestSubscriber subscriber = componentProvider
            .getInstance(ParameterizedStringTestSubscriber.class);
        subscriber.prepareForTest(latch, receivedRef);
        // when
        jedisServer.publish(channel, expectedMessage, userId);
        // then
        final boolean messageArrived = latch.await(3, TimeUnit.SECONDS);
        assertTrue(messageArrived, "Did not receive parameterized pub/sub message within timeout");
        assertEquals(expectedMessage, receivedRef.get(),
            "Received message does not match published message");
    }

    @Test
    @DisplayName("should auto-discover, register and receive binary message via DI pub/sub channel")
    void shouldPublishAndReceiveBinaryMessage() throws InterruptedException {
        // given
        final byte[] expectedPayload = "DI_Auto_Discovery_Binary_Payload"
            .getBytes(StandardCharsets.UTF_8);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<byte[]> receivedRef = new AtomicReference<>();
        final SimpleBinaryTestSubscriber subscriber = componentProvider
            .getInstance(SimpleBinaryTestSubscriber.class);
        subscriber.prepareForTest(latch, receivedRef);
        // when
        jedisServer.publishBinary(TestKvChannel.TEST_EVENTS, expectedPayload);
        // then
        final boolean messageArrived = latch.await(3, TimeUnit.SECONDS);
        assertTrue(messageArrived, "Did not receive binary pub/sub message within timeout");
        assertNotNull(receivedRef.get(), "Received binary message should not be null");
        assertArrayEquals(expectedPayload, receivedRef.get());
    }

    @Test
    @DisplayName("should auto-discover pattern subscriber and extract dynamic parameters from channel")
    void shouldExtractWildcardParamsEndToEnd() throws InterruptedException {
        // given
        final String targetUserId = "player_777";
        final String expectedMessage = "Level_Up_Event";
        final KvChannel exactChannel = params -> "integration:users:" + targetUserId + ":events";
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> receivedRef = new AtomicReference<>();
        final AtomicReference<String[]> paramsRef = new AtomicReference<>();
        final PatternStringTestSubscriber subscriber = componentProvider
            .getInstance(PatternStringTestSubscriber.class);
        subscriber.prepareForTest(latch, receivedRef, paramsRef);
        // when
        jedisServer.publish(exactChannel, expectedMessage);
        // then
        final boolean messageArrived = latch.await(3, TimeUnit.SECONDS);
        assertTrue(messageArrived, "Did not receive Pattern message within timeout");
        assertEquals(expectedMessage, receivedRef.get(), "Received message does not match");
        assertNotNull(paramsRef.get(), "Params array should not be null");
        assertEquals(1, paramsRef.get().length, "Should extract exactly one parameter");
        assertEquals(targetUserId, paramsRef.get()[0], "Extracted wildcard param does not match!");
    }
}
