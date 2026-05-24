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
package xyz.jwizard.jwl.websocket.jetty;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;
import org.eclipse.jetty.websocket.server.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.common.limit.RateLimiter;
import xyz.jwizard.jwl.net.bus.RawBusListener;
import xyz.jwizard.jwl.net.lifecycle.NetworkSessionLifecycleListener;
import xyz.jwizard.jwl.websocket.WsHandshakeRequest;
import xyz.jwizard.jwl.websocket.WsSession;
import xyz.jwizard.jwl.websocket.auth.WsAuthenticator;
import xyz.jwizard.jwl.websocket.auth.handler.WsAuthFailureHandler;
import xyz.jwizard.jwl.websocket.jetty.adapter.JettyWsHandshakeRequestAdapter;
import xyz.jwizard.jwl.websocket.jetty.adapter.JettyWsListenerAdapter;
import xyz.jwizard.jwl.websocket.negotation.WsSerializerResolver;
import xyz.jwizard.jwl.websocket.registry.WsSessionRegistry;

public class JettyWsCreator implements WebSocketCreator {
    private static final Logger LOG = LoggerFactory.getLogger(JettyWsCreator.class);

    private final NetworkSessionLifecycleListener<WsSession> lifecycleListener;
    private final RawBusListener<WsSession> busListener;
    private final WsSessionRegistry sessionRegistry;
    private final WsAuthenticator authenticator;
    private final WsAuthFailureHandler failureHandler;
    private final RateLimiter rateLimiter;
    private final WsSerializerResolver serializerResolver;

    public JettyWsCreator(NetworkSessionLifecycleListener<WsSession> lifecycleListener,
                          RawBusListener<WsSession> busListener, WsSessionRegistry sessionRegistry,
                          WsAuthenticator authenticator, WsAuthFailureHandler failureHandler,
                          RateLimiter rateLimiter, WsSerializerResolver serializerResolver) {
        this.lifecycleListener = lifecycleListener;
        this.busListener = busListener;
        this.sessionRegistry = sessionRegistry;
        this.authenticator = authenticator;
        this.failureHandler = failureHandler;
        this.rateLimiter = rateLimiter;
        this.serializerResolver = serializerResolver;
    }

    @Override
    public Object createWebSocket(ServerUpgradeRequest req, ServerUpgradeResponse res,
                                  Callback callback) {
        LOG.trace("Intercepted WebSocket upgrade request, initiating authentication");
        final WsHandshakeRequest handshakeRequest = new JettyWsHandshakeRequestAdapter(req);

        final EnvelopeSerializer<?> serializer = serializerResolver.resolve(handshakeRequest);
        if (serializer == null) {
            LOG.debug("WebSocket negotiation failed: missing or unsupported encoding parameter");
            res.setStatus(HttpStatus.BAD_REQUEST_400);
            callback.succeeded();
            return null;
        }
        final String principalId = authenticator.authenticate(handshakeRequest);
        if (principalId != null) {
            LOG.debug("WebSocket authentication successful for principal: {}", principalId);
            return new JettyWsListenerAdapter(
                lifecycleListener,
                busListener,
                sessionRegistry,
                rateLimiter,
                serializer,
                principalId
            );
        }
        LOG.debug("WebSocket authentication failed, rejecting connection with HTTP 401");
        if (failureHandler != null) {
            try {
                failureHandler.onAuthFailure(handshakeRequest);
            } catch (Exception ex) {
                LOG.error("Exception thrown during WsAuthFailureHandler execution, " +
                    "proceeding to close socket", ex);
            }
        }
        res.setStatus(HttpStatus.UNAUTHORIZED_401);
        callback.succeeded();
        return null;
    }
}
