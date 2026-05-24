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
package xyz.jwizard.jwl.net.ws;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.UnsupportedDataTypeException;
import xyz.jwizard.jwl.common.registry.RegistryTracker;
import xyz.jwizard.jwl.common.util.concurrent.ConcurrentOperationException;
import xyz.jwizard.jwl.common.util.io.RunnableWithException;
import xyz.jwizard.jwl.net.CloseCode;
import xyz.jwizard.jwl.net.NetworkSession;
import xyz.jwizard.jwl.net.bus.RawBusListener;
import xyz.jwizard.jwl.net.lifecycle.NetworkSessionLifecycleListener;

public abstract class GenericWsListenerHandler<S extends NetworkSession> {
    private static final String UNKNOWN_SESSION = "UNKNOWN_PENDING_SESSION";

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final RegistryTracker<S> registryTracker;
    protected final NetworkSessionLifecycleListener<S> lifecycleListener;
    protected final RawBusListener<S> busListener;

    protected S sessionAdapter;

    protected GenericWsListenerHandler(RegistryTracker<S> registryTracker,
                                       NetworkSessionLifecycleListener<S> lifecycleListener,
                                       RawBusListener<S> busListener) {
        this.registryTracker = registryTracker;
        this.lifecycleListener = lifecycleListener;
        this.busListener = busListener;
    }

    protected void handleConnect() {
        try {
            if (log.isTraceEnabled()) {
                log.trace("Invoking onConnect for session: {}", getSafeSessionId());
            }
            lifecycleListener.onConnect(sessionAdapter);
        } catch (Exception ex) {
            handleFatalError("Exception during onConnect phase", ex);
        }
    }

    protected void handleClose(int statusCode, String reason) {
        log.debug("WebSocket connection closed, session: {}, status: {}, reason: '{}'",
            getSafeSessionId(), statusCode, reason);
        if (sessionAdapter != null) {
            cleanupSession();
            lifecycleListener.onClose(sessionAdapter, statusCode, reason);
        }
    }

    protected void handleError(Throwable cause) {
        log.warn("WebSocket protocol/network error, session: {}, cause: {}", getSafeSessionId(),
            cause != null ? cause.getMessage() : "null");
        if (sessionAdapter != null) {
            cleanupSession();
            lifecycleListener.onError(sessionAdapter, cause);
        }
    }

    protected void onText(String message) {
        if (log.isTraceEnabled()) {
            log.trace("Received text message, session: {}, size: {} chars", getSafeSessionId(),
                message.length());
        }
        processMessageSafe(() -> busListener.dispatch(sessionAdapter, message), null, null);
    }

    protected void onBinary(ByteBuffer payload, Runnable onSuccess, Consumer<Throwable> onFailure) {
        final byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        if (log.isTraceEnabled()) {
            log.trace("Received binary message, session: {}, size: {} bytes", getSafeSessionId(),
                bytes.length);
        }
        processMessageSafe(() -> busListener.dispatch(sessionAdapter, bytes), onSuccess, onFailure);
    }

    protected void processMessageSafe(RunnableWithException action, Runnable onSuccess,
                                      Consumer<Throwable> onFailure) {
        final String sessionId = getSafeSessionId();
        try {
            if (!checkRateLimit()) {
                log.warn("Rate limit exceeded for session {}, dropping message.", sessionId);
                onRateLimitExceeded();
                runOnSuccess(onSuccess);
                return;
            }
            action.run();
            runOnSuccess(onSuccess);
        } catch (UnsupportedDataTypeException ex) {
            handleFatalProcessingError(ex, WsCloseCode.UNSUPPORTED_FRAME_TYPE, onSuccess);
        } catch (ConcurrentOperationException ex) {
            handleFatalProcessingError(ex, WsCloseCode.SERVER_OVERLOADED, onSuccess);
        } catch (RuntimeException ex) {
            lifecycleListener.onError(sessionAdapter, ex);
            log.error("Business logic error in session: {}", sessionId, ex);
            onBusinessError();
            runOnSuccess(onSuccess);
        } catch (Exception ex) {
            handleFatalError("Exception during message processing", ex);
            if (onFailure != null) {
                onFailure.accept(ex);
            }
        }
    }

    private void handleFatalProcessingError(Exception ex, CloseCode closeCode, Runnable onSuccess) {
        lifecycleListener.onError(sessionAdapter, ex);
        if (sessionAdapter != null) {
            sessionAdapter.close(closeCode);
        }
        runOnSuccess(onSuccess);
    }

    private void handleFatalError(String context, Exception ex) {
        log.error("Critical error, context: '{}', session: {}.", context, getSafeSessionId(), ex);
        if (sessionAdapter != null) {
            lifecycleListener.onError(sessionAdapter, ex);
            sessionAdapter.close(WsCloseCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void runOnSuccess(Runnable onSuccess) {
        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    protected String getSafeSessionId() {
        return sessionAdapter != null ? sessionAdapter.getSessionId() : UNKNOWN_SESSION;
    }

    protected void cleanupSession() {
        registryTracker.unregister(sessionAdapter);
    }

    protected boolean checkRateLimit() {
        return true;
    }

    protected void onRateLimitExceeded() {
    }

    protected void onBusinessError() {
    }
}
