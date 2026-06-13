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
package xyz.noisif.nsl.netclient.websocket.heartbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.util.concurrent.PeriodicTaskScheduler;
import xyz.noisif.nsl.netclient.websocket.WsClientSession;
import xyz.noisif.nsl.netclient.websocket.group.heartbeat.WsHeartbeatConfig;

import java.util.concurrent.TimeUnit;

public class WsHeartbeatManager {
  private static final Logger LOG = LoggerFactory.getLogger(WsHeartbeatManager.class);

  private final PeriodicTaskScheduler scheduler;

  public WsHeartbeatManager(PeriodicTaskScheduler scheduler) {
    this.scheduler = scheduler;
  }

  public void start(WsClientSession session, WsHeartbeatConfig config) {
    if (config == null || session == null) {
      return;
    }
    final long intervalMs = config.getInterval().toMillis();
    LOG.debug(
        "Starting WS heartbeat for session {} (interval: {} ms)",
        session.getSessionId(),
        intervalMs);
    scheduler.scheduleAtFixedRate(
        session.getSessionId(),
        new HeartbeatTask(session, config.getAction(), this),
        intervalMs,
        intervalMs,
        TimeUnit.MILLISECONDS);
  }

  public void stop(String sessionId) {
    if (sessionId == null) {
      return;
    }
    LOG.debug("Stopping WS heartbeat for session {}", sessionId);
    scheduler.cancel(sessionId);
  }

  public void stopAll() {
    LOG.debug("Terminating all active heartbeat tasks.");
    scheduler.cancelAll();
  }
}
