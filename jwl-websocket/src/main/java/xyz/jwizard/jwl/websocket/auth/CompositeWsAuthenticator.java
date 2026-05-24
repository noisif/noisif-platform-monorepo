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
package xyz.jwizard.jwl.websocket.auth;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.websocket.WsHandshakeRequest;

public class CompositeWsAuthenticator implements WsAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeWsAuthenticator.class);

    private final List<WsAuthenticator> authenticators;

    public CompositeWsAuthenticator(List<WsAuthenticator> authenticators) {
        this.authenticators = authenticators;
    }

    @Override
    public String authenticate(WsHandshakeRequest req) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting composite authentication, evaluating {} authenticators",
                authenticators.size());
        }
        for (final WsAuthenticator authenticator : authenticators) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Evaluating request using: {}", authenticator.getClass().getSimpleName());
            }
            final String principalId = authenticator.authenticate(req);
            if (principalId != null) {
                LOG.debug("Authentication successful via {}, principalId: {}",
                    authenticator.getClass().getSimpleName(), principalId);
                return principalId;
            }
        }
        LOG.debug("Composite authentication failed, " +
            "no authenticator was able to identify the principal");
        return null;
    }
}
