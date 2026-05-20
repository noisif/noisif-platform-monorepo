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

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskExecutor implements Executor, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(TaskExecutor.class);

    private final ExecutorService delegate;
    private final String name;
    private final long timeout;
    private final TimeUnit unit;

    private TaskExecutor(String name, long timeout, TimeUnit unit) {
        this.delegate = Executors.newVirtualThreadPerTaskExecutor();
        this.name = name;
        this.timeout = timeout;
        this.unit = unit;
        LOG.debug("Initialized virtual thread executor: {}", name);
    }

    public static TaskExecutor create(String name, Duration timeout) {
        return new TaskExecutor(name, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public static TaskExecutor createDefault(String name) {
        return new TaskExecutor(name, 30, TimeUnit.SECONDS);
    }

    public ExecutorService getDelegate() {
        return delegate;
    }

    @Override
    public void execute(@NonNull Runnable command) {
        delegate.execute(command);
    }

    @Override
    public void close() throws IOException {
        if (delegate == null || delegate.isShutdown()) {
            return;
        }
        LOG.debug("Initiating graceful shutdown of executor: {}", name);
        delegate.shutdown();
        try {
            if (!delegate.awaitTermination(timeout, unit)) {
                LOG.warn("Executor '{}' did not terminate in time. forcing shutdown", name);
                delegate.shutdownNow();
                if (!delegate.awaitTermination(timeout, unit)) {
                    LOG.error("Executor '{}' did not terminate even after forcing", name);
                }
            }
        } catch (InterruptedException ignored) {
            LOG.error("Shutdown of executor '{}' interrupted", name);
            delegate.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.debug("Executor '{}' has been shut down", name);
    }
}
