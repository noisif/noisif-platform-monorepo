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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualPeriodicTaskScheduler implements PeriodicTaskScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualPeriodicTaskScheduler.class);

    private final ScheduledExecutorService ticker;
    private final ExecutorService virtualWorkers;
    private final Map<String, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    private VirtualPeriodicTaskScheduler(ScheduledExecutorService ticker,
                                         ExecutorService virtualWorkers) {
        this.ticker = ticker;
        this.virtualWorkers = virtualWorkers;
    }

    public static PeriodicTaskScheduler create(ScheduledExecutorService ticker,
                                               ExecutorService virtualWorkers) {
        return new VirtualPeriodicTaskScheduler(ticker, virtualWorkers);
    }

    @Override
    public void scheduleAtFixedRate(String taskId, Runnable task, long initialDelay, long period,
                                    TimeUnit unit) {
        if (taskId == null || task == null) {
            return;
        }
        LOG.debug("Scheduling periodic task '{}' with initial delay {}ms, period {}ms", taskId,
            unit.toMillis(initialDelay), unit.toMillis(period));
        final ScheduledFuture<?> future = ticker.scheduleAtFixedRate(() -> {
            try {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Ticker triggered task: '{}'", taskId);
                }
                virtualWorkers.execute(task);
            } catch (Exception ex) {
                LOG.error("Failed to submit periodic task '{}' to virtual worker pool", taskId, ex);
            }
        }, initialDelay, period, unit);
        final ScheduledFuture<?> previous = activeTasks.put(taskId, future);
        if (previous != null) {
            LOG.debug("Task '{}' was already scheduled, replacing existing future", taskId);
            previous.cancel(false);
        }
    }

    @Override
    public void cancel(String taskId) {
        if (taskId == null) {
            return;
        }
        final ScheduledFuture<?> future = activeTasks.remove(taskId);
        if (future != null) {
            LOG.debug("Cancelling periodic task: '{}'", taskId);
            future.cancel(false);
        }
    }

    @Override
    public void cancelAll() {
        LOG.debug("Cancelling all {} active periodic tasks", activeTasks.size());
        for (final ScheduledFuture<?> future : activeTasks.values()) {
            future.cancel(true);
        }
        activeTasks.clear();
    }
}
