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
package xyz.jwizard.jwl.netclient.websocket.jetty.adapter;

import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import xyz.jwizard.jwl.net.lifecycle.NetworkSessionLifecycleListener;
import xyz.jwizard.jwl.net.ws.GenericWsListenerHandler;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.websocket.WsClientSession;
import xyz.jwizard.jwl.netclient.websocket.group.WsClientGroupConfig;
import xyz.jwizard.jwl.netclient.websocket.group.codec.WsSessionCodec;
import xyz.jwizard.jwl.netclient.websocket.registry.WsClientSessionRegistry;

public class JettyWsClientListenerAdapter extends GenericWsListenerHandler<WsClientSession>
    implements Session.Listener.AutoDemanding {
    private final ClientGroup clientGroup;
    private final WsClientGroupConfig groupConfig;
    private final WsClientSessionRegistry sessionRegistry;
    private final WsSessionCodec sessionCodec;

    public JettyWsClientListenerAdapter(ClientGroup clientGroup, WsClientGroupConfig groupConfig,
                                        WsClientSessionRegistry sessionRegistry,
                                        WsSessionCodec sessionCodec,
                                        NetworkSessionLifecycleListener<WsClientSession>
                                            lifecycleListener) {
        super(sessionRegistry, lifecycleListener, groupConfig.getBusConfig().getBusListener());
        this.clientGroup = clientGroup;
        this.groupConfig = groupConfig;
        this.sessionRegistry = sessionRegistry;
        this.sessionCodec = sessionCodec;
    }

    @Override
    public void onWebSocketOpen(Session session) {
        sessionAdapter = new JettyWsClientSessionAdapter(
            clientGroup, session, groupConfig.getPrincipalId(), sessionCodec
        );
        sessionRegistry.register(sessionAdapter);
        super.handleConnect();
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.handleError(cause);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason, Callback callback) {
        try {
            super.handleClose(statusCode, reason);
        } finally {
            callback.succeed();
        }
    }

    @Override
    public void onWebSocketText(String message) {
        super.onText(message);
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        super.onBinary(payload,
            () -> completeCallback(callback, null),
            ex -> completeCallback(callback, ex)
        );
    }

    private void completeCallback(Callback callback, Throwable error) {
        if (callback == null) {
            return;
        }
        if (error == null) {
            callback.succeed();
            return;
        }
        callback.fail(error);
    }
}
