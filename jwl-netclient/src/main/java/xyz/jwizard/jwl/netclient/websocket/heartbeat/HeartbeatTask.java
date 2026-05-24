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
