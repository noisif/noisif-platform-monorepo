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
package xyz.jwizard.jwl.websocket.jetty.adapter;

import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.StandardOpCode;
import xyz.jwizard.jwl.common.limit.RateLimiter;
import xyz.jwizard.jwl.net.bus.RawBusListener;
import xyz.jwizard.jwl.net.lifecycle.NetworkSessionLifecycleListener;
import xyz.jwizard.jwl.net.ws.GenericWsListenerHandler;
import xyz.jwizard.jwl.websocket.WsSession;
import xyz.jwizard.jwl.websocket.registry.WsSessionRegistry;

public class JettyWsListenerAdapter extends GenericWsListenerHandler<WsSession>
    implements Session.Listener.AutoDemanding {
    private final WsSessionRegistry registry;
    private final RateLimiter rateLimiter;
    private final EnvelopeSerializer<?> envelopeSerializer;
    private final String principalId;

    public JettyWsListenerAdapter(NetworkSessionLifecycleListener<WsSession> lifecycleListener,
                                  RawBusListener<WsSession> busListener,
                                  WsSessionRegistry registry, RateLimiter rateLimiter,
                                  EnvelopeSerializer<?> envelopeSerializer, String principalId) {
        super(registry, lifecycleListener, busListener);
        this.registry = registry;
        this.rateLimiter = rateLimiter;
        this.envelopeSerializer = envelopeSerializer;
        this.principalId = principalId;
    }

    @Override
    public void onWebSocketOpen(Session session) {
        log.debug("WS connection opening for principal: {}", principalId);
        sessionAdapter = new JettyWsSessionAdapter(session, principalId, envelopeSerializer);
        registry.register(sessionAdapter);
        super.handleConnect();
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
    public void onWebSocketError(Throwable cause) {
        super.handleError(cause);
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
        } else {
            callback.fail(error);
        }
    }

    @Override
    protected void cleanupSession() {
        super.cleanupSession();
        rateLimiter.reset(principalId);
    }

    @Override
    protected boolean checkRateLimit() {
        return rateLimiter.tryAcquire(principalId);
    }

    @Override
    protected void onRateLimitExceeded() {
        sessionAdapter.sendEnvelope(StandardOpCode.RATE_LIMIT_EXCEEDED);
    }

    @Override
    protected void onBusinessError() {
        sessionAdapter.sendEnvelope(StandardOpCode.INTERNAL_ERROR);
    }
}
