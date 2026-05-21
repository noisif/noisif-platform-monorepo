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
package xyz.jwizard.jwl.netclient.websocket.heartbeat;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.util.concurrent.PeriodicTaskScheduler;
import xyz.jwizard.jwl.netclient.websocket.WsClientSession;
import xyz.jwizard.jwl.netclient.websocket.group.heartbeat.WsHeartbeatConfig;

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
        LOG.debug("Starting WS heartbeat for session {} (interval: {} ms)", session.getSessionId(),
            intervalMs);
        scheduler.scheduleAtFixedRate(
            session.getSessionId(),
            new HeartbeatTask(session, config.getAction(), this),
            intervalMs,
            intervalMs,
            TimeUnit.MILLISECONDS
        );
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
