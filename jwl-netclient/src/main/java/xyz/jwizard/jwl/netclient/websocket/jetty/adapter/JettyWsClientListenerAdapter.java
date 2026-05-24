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
