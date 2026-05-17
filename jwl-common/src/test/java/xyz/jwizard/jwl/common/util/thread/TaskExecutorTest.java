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
package xyz.jwizard.jwl.common.util.thread;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.common.util.io.IoUtil;

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
        executor = new TaskExecutor("test-executor");
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean executed = new AtomicBoolean(false);
        // when
        executor.execute(() -> {
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
        executor = new TaskExecutor("shutdown-test", 1, TimeUnit.SECONDS);
        final CountDownLatch taskStarted = new CountDownLatch(1);
        final CountDownLatch taskFinished = new CountDownLatch(1);
        executor.execute(() -> {
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
        executor = new TaskExecutor("timeout-test", 100, TimeUnit.MILLISECONDS);
        final CountDownLatch taskStarted = new CountDownLatch(1);
        final AtomicBoolean interrupted = new AtomicBoolean(false);
        executor.execute(() -> {
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
