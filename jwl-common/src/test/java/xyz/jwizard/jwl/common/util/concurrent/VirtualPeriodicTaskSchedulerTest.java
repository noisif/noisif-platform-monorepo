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
package xyz.jwizard.jwl.common.util.concurrent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.common.util.thread.TaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class VirtualPeriodicTaskSchedulerTest {
  private ScheduledExecutorService ticker;
  private TaskExecutor taskExecutor;
  private PeriodicTaskScheduler scheduler;

  @BeforeEach
  void setUp() {
    ticker = ConcurrentUtil.singleThread("test-ticker");
    taskExecutor = TaskExecutor.createDefault("test-worker");
    scheduler = VirtualPeriodicTaskScheduler.create(ticker, taskExecutor.getDelegate());
  }

  @AfterEach
  void tearDown() {
    scheduler.cancelAll();
    IoUtil.closeQuietly(taskExecutor);
    ticker.shutdownNow();
  }

  @Test
  @DisplayName("should execute task multiple times using virtual threads")
  void shouldExecuteTaskMultipleTimes() throws InterruptedException {
    // given
    final int expectedExecutions = 3;
    final CountDownLatch latch = new CountDownLatch(expectedExecutions);
    final AtomicInteger counter = new AtomicInteger(0);
    // when
    scheduler.scheduleAtFixedRate(
        "test-task",
        () -> {
          counter.incrementAndGet();
          latch.countDown();
        },
        10,
        50,
        TimeUnit.MILLISECONDS);
    // then
    final boolean completed = latch.await(1, TimeUnit.SECONDS);
    assertThat(completed).isTrue();
    assertThat(counter.get()).isGreaterThanOrEqualTo(expectedExecutions);
  }

  @Test
  @DisplayName("should stop executing task after cancellation")
  void shouldStopExecutionAfterCancel() throws InterruptedException {
    // given
    final AtomicInteger counter = new AtomicInteger(0);
    final String taskId = "cancellable-task";
    // when
    scheduler.scheduleAtFixedRate(taskId, counter::incrementAndGet, 10, 50, TimeUnit.MILLISECONDS);
    Thread.sleep(150);
    scheduler.cancel(taskId);
    final int countAfterCancel = counter.get();
    Thread.sleep(150);
    // then
    assertThat(counter.get()).isEqualTo(countAfterCancel);
  }
}
