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
package xyz.jwizard.jwl.websocket.jetty.adapter;

import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.UnsupportedEnvelopeDataTypeException;
import xyz.jwizard.jwl.common.limit.RateLimiter;
import xyz.jwizard.jwl.common.util.concurrent.ConcurrentOperationException;
import xyz.jwizard.jwl.common.util.io.RunnableWithException;
import xyz.jwizard.jwl.websocket.WsSession;
import xyz.jwizard.jwl.websocket.listener.WsMessageListener;
import xyz.jwizard.jwl.websocket.listener.action.WsOpCode;
import xyz.jwizard.jwl.websocket.listener.lifecycle.WsLifecycleListener;
import xyz.jwizard.jwl.websocket.registry.WsSessionRegistry;

public class JettyWsListenerAdapter implements Session.Listener.AutoDemanding {
    private static final Logger LOG = LoggerFactory.getLogger(JettyWsListenerAdapter.class);

    private final WsLifecycleListener lifecycleListener;
    private final WsMessageListener messageListener;
    private final WsSessionRegistry registry;
    private final RateLimiter rateLimiter;
    private final EnvelopeSerializer<?> envelopeSerializer;
    private final String principalId;

    private WsSession sessionAdapter;

    public JettyWsListenerAdapter(WsLifecycleListener lifecycleListener,
                                  WsMessageListener messageListener, WsSessionRegistry registry,
                                  RateLimiter rateLimiter, EnvelopeSerializer<?> envelopeSerializer,
                                  String principalId) {
        this.lifecycleListener = lifecycleListener;
        this.messageListener = messageListener;
        this.registry = registry;
        this.rateLimiter = rateLimiter;
        this.envelopeSerializer = envelopeSerializer;
        this.principalId = principalId;
    }

    @Override
    public void onWebSocketOpen(Session session) {
        LOG.debug("WebSocket connection opening for principal: {}", principalId);
        sessionAdapter = new JettyWsSessionAdapter(session, principalId, envelopeSerializer);
        registry.register(sessionAdapter);
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Invoking domainHandler.onConnect for session: {}", getSafeSessionId());
            }
            lifecycleListener.onConnect(sessionAdapter);
        } catch (Exception ex) {
            handleFatalError("Exception during onConnect phase", ex);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason, Callback callback) {
        final String sessionId = getSafeSessionId();
        LOG.debug("WebSocket connection closed, session: {}, status: {}, reason: '{}'",
            sessionId, statusCode, reason);
        try {
            if (sessionAdapter != null) {
                rateLimiter.reset(principalId);
                registry.unregister(sessionAdapter);
                lifecycleListener.onClose(sessionAdapter, statusCode, reason);
            }
        } finally {
            callback.succeed();
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        LOG.warn("WebSocket protocol/network error, session: {}, cause: {}", getSafeSessionId(),
            cause.getMessage());
        if (sessionAdapter != null) {
            registry.unregister(sessionAdapter);
            lifecycleListener.onError(sessionAdapter, cause);
        }
    }

    @Override
    public void onWebSocketText(String message) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Received text message, session: {}, size: {} chars", getSafeSessionId(),
                message.length());
        }
        processMessageInternal(null, () -> messageListener.onMessage(sessionAdapter, message));
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        final byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Received binary message, session: {}, size: {} bytes", getSafeSessionId(),
                bytes.length);
        }
        processMessageInternal(callback, () -> messageListener.onMessage(sessionAdapter, bytes));
    }

    private void processMessageInternal(Callback callback, RunnableWithException action) {
        final String sessionId = getSafeSessionId();
        try {
            if (!rateLimiter.tryAcquire(principalId)) {
                LOG.warn("Rate limit exceeded for session {}, dropping message.", sessionId);
                if (sessionAdapter != null) {
                    sessionAdapter.sendEnvelope(WsOpCode.RATE_LIMIT_EXCEEDED);
                }
                completeCallback(callback, null);
                return;
            }
            action.run();
            completeCallback(callback, null);
        } catch (UnsupportedEnvelopeDataTypeException ex) {
            handleFatalProcessingError(ex, 1003, "Unsupported Frame Type", callback);
        } catch (ConcurrentOperationException ex) {
            handleFatalProcessingError(ex, 1011, "Server Overloaded", callback);
        } catch (RuntimeException ex) {
            lifecycleListener.onError(sessionAdapter, ex);
            LOG.error("Business logic error in session: {}", sessionId, ex);
            if (sessionAdapter != null) {
                sessionAdapter.sendEnvelope(WsOpCode.INTERNAL_ERROR);
            }
            completeCallback(callback, null);
        } catch (Exception ex) {
            handleFatalError("Exception during message processing", ex);
            completeCallback(callback, ex);
        }
    }

    private void handleFatalProcessingError(Exception ex, int closeCode, String closeReason,
                                            Callback callback) {
        lifecycleListener.onError(sessionAdapter, ex);
        if (sessionAdapter != null) {
            sessionAdapter.close(closeCode, closeReason);
        }
        completeCallback(callback, null);
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

    private void handleFatalError(String context, Exception ex) {
        LOG.error("Critical error, context: '{}', session: {}.", context,
            getSafeSessionId(), ex);
        if (sessionAdapter != null) {
            lifecycleListener.onError(sessionAdapter, ex);
            sessionAdapter.close(1011, "Internal Server Error");
        }
    }

    private String getSafeSessionId() {
        return sessionAdapter != null ? sessionAdapter.getSessionId() : "UNKNOWN_PENDING_SESSION";
    }
}
