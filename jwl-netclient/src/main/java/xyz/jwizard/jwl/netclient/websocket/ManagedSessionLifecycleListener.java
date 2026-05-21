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
package xyz.jwizard.jwl.netclient.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.net.lifecycle.NetworkSessionLifecycleListener;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.websocket.group.WsClientGroupConfig;
import xyz.jwizard.jwl.netclient.websocket.heartbeat.WsHeartbeatManager;

class ManagedSessionLifecycleListener implements NetworkSessionLifecycleListener<WsClientSession> {
    private static final Logger LOG = LoggerFactory.getLogger(ManagedSessionLifecycleListener.class);

    private final ClientGroup clientGroup;
    private final WsClientGroupConfig config;
    private final WsHeartbeatManager heartbeatManager;
    private final WsReconnectManager reconnectManager;
    private final Runnable reconnectTrigger;

    ManagedSessionLifecycleListener(ClientGroup clientGroup, WsClientGroupConfig config,
                                    WsHeartbeatManager heartbeatManager,
                                    WsReconnectManager reconnectManager,
                                    Runnable reconnectTrigger) {
        this.clientGroup = clientGroup;
        this.config = config;
        this.heartbeatManager = heartbeatManager;
        this.reconnectManager = reconnectManager;
        this.reconnectTrigger = reconnectTrigger;
    }

    @Override
    public void onConnect(WsClientSession session) {
        LOG.info("WS connected for group '{}'. session id: {}", clientGroup.getClientGroupName(),
            session.getSessionId());
        heartbeatManager.start(session, config.getHeartbeatConfig());
        config.getLifecycleListener().onConnect(session);
    }

    @Override
    public void onClose(WsClientSession session, int statusCode, String reason) {
        LOG.info("WS closed for group '{}' [code: {}], session id: {}",
            clientGroup.getClientGroupName(), statusCode, session.getSessionId());
        heartbeatManager.stop(session.getSessionId());
        config.getLifecycleListener().onClose(session, statusCode, reason);
        reconnectManager.handleDisconnect(clientGroup, config, statusCode, reconnectTrigger);
    }

    @Override
    public void onError(WsClientSession session, Throwable cause) {
        LOG.warn("WS error for group '{}': {}", clientGroup.getClientGroupName(),
            cause.getMessage());
        config.getLifecycleListener().onError(session, cause);
    }
}
