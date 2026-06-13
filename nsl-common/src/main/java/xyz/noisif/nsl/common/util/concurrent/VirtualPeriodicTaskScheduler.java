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
package xyz.noisif.nsl.common.util.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class VirtualPeriodicTaskScheduler implements PeriodicTaskScheduler {
  private static final Logger LOG = LoggerFactory.getLogger(VirtualPeriodicTaskScheduler.class);

  private final ScheduledExecutorService ticker;
  private final ExecutorService virtualWorkers;
  private final Map<String, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

  private VirtualPeriodicTaskScheduler(
      ScheduledExecutorService ticker, ExecutorService virtualWorkers) {
    this.ticker = ticker;
    this.virtualWorkers = virtualWorkers;
  }

  public static PeriodicTaskScheduler create(
      ScheduledExecutorService ticker, ExecutorService virtualWorkers) {
    return new VirtualPeriodicTaskScheduler(ticker, virtualWorkers);
  }

  @Override
  public void scheduleAtFixedRate(
      String taskId, Runnable task, long initialDelay, long period, TimeUnit unit) {
    if (taskId == null || task == null) {
      return;
    }
    LOG.debug(
        "Scheduling periodic task '{}' with initial delay {}ms, period {}ms",
        taskId,
        unit.toMillis(initialDelay),
        unit.toMillis(period));
    final ScheduledFuture<?> future =
        ticker.scheduleAtFixedRate(
            () -> {
              try {
                if (LOG.isTraceEnabled()) {
                  LOG.trace("Ticker triggered task: '{}'", taskId);
                }
                virtualWorkers.execute(task);
              } catch (Exception ex) {
                LOG.error("Failed to submit periodic task '{}' to virtual worker pool", taskId, ex);
              }
            },
            initialDelay,
            period,
            unit);
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
