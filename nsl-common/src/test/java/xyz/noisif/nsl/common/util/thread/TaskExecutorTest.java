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
package xyz.noisif.nsl.common.util.thread;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.noisif.nsl.common.util.io.IoUtil;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class TaskExecutorTest {
  private TaskExecutor executor;

  @AfterEach
  void tearDown() {
    IoUtil.closeQuietly(executor);
  }

  @Test
  @DisplayName("should execute task using virtual threads")
  void shouldExecuteTask() throws InterruptedException {
    // given
    executor = TaskExecutor.createDefault("test-executor");
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean executed = new AtomicBoolean(false);
    // when
    executor.execute(
        () -> {
          executed.set(true);
          latch.countDown();
        });
    // then
    final boolean finished = latch.await(2, TimeUnit.SECONDS);
    assertThat(finished).as("Task should have finished before timeout").isTrue();
    assertThat(executed.get()).isTrue();
  }

  @Test
  @DisplayName("should wait for tasks to finish during graceful shutdown")
  void shouldShutdownGracefully() throws IOException, InterruptedException {
    // given
    executor = TaskExecutor.create("shutdown-test", Duration.ofSeconds(1));
    final CountDownLatch taskStarted = new CountDownLatch(1);
    final CountDownLatch taskFinished = new CountDownLatch(1);
    executor.execute(
        () -> {
          taskStarted.countDown();
          try {
            Thread.sleep(500);
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
          }
          taskFinished.countDown();
        });
    boolean started = taskStarted.await(1, TimeUnit.SECONDS);
    assertThat(started).as("Task should have started").isTrue();
    // when
    executor.close();
    // then
    assertThat(taskFinished.getCount()).as("Task should finish before close returns").isZero();
    assertThat(executor.getDelegate().isShutdown()).isTrue();
  }

  @Test
  @DisplayName("should force shutdown when tasks exceed timeout")
  void shouldForceShutdownOnTimeout() throws IOException, InterruptedException {
    // given
    executor = TaskExecutor.create("timeout-test", Duration.ofMillis(100));
    final CountDownLatch taskStarted = new CountDownLatch(1);
    final AtomicBoolean interrupted = new AtomicBoolean(false);
    executor.execute(
        () -> {
          taskStarted.countDown();
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            interrupted.set(true);
          }
        });
    boolean started = taskStarted.await(1, TimeUnit.SECONDS);
    assertThat(started).as("Long task should have started").isTrue();
    // when
    executor.close();
    // then
    assertThat(executor.getDelegate().isShutdown()).isTrue();
    assertThat(interrupted.get()).as("Task should be interrupted due to timeout").isTrue();
  }
}
