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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.netclient.websocket.WsClientSession;
import xyz.jwizard.jwl.netclient.websocket.group.heartbeat.WsHeartbeatAction;

class HeartbeatTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(HeartbeatTask.class);

    private final WsClientSession session;
    private final WsHeartbeatAction action;
    private final WsHeartbeatManager manager;

    HeartbeatTask(WsClientSession session, WsHeartbeatAction action, WsHeartbeatManager manager) {
        this.session = session;
        this.action = action;
        this.manager = manager;
    }

    @Override
    public void run() {
        if (session.isClosed()) {
            manager.stop(session.getSessionId());
            return;
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Triggering heartbeat for session {}", session.getSessionId());
        }
        try {
            action.execute(session);
        } catch (Exception ex) {
            LOG.warn("Failed to dispatch heartbeat for session {}: {}", session.getSessionId(),
                ex.getMessage());
        }
    }
}
