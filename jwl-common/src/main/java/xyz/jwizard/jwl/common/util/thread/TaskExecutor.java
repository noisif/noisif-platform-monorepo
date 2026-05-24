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
