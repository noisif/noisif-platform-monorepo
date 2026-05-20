/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jwizard.jwl.common.util.concurrent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.common.util.thread.TaskExecutor;

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
        scheduler.scheduleAtFixedRate("test-task", () -> {
            counter.incrementAndGet();
            latch.countDown();
        }, 10, 50, TimeUnit.MILLISECONDS);
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
        scheduler.scheduleAtFixedRate(taskId, counter::incrementAndGet, 10, 50,
            TimeUnit.MILLISECONDS);
        Thread.sleep(150);
        scheduler.cancel(taskId);
        final int countAfterCancel = counter.get();
        Thread.sleep(150);
        // then
        assertThat(counter.get()).isEqualTo(countAfterCancel);
    }
}
