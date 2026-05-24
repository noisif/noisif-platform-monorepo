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
package xyz.jwizard.jwl.netclient.websocket.auth;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.netclient.websocket.WsClientUpgradeRequest;

public class CompositeWsClientAuthenticator implements WsClientAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeWsClientAuthenticator.class);

    private final List<WsClientAuthenticator> authenticators;

    private CompositeWsClientAuthenticator(List<WsClientAuthenticator> authenticators) {
        this.authenticators = authenticators;
    }

    public static WsClientAuthenticator load(Set<WsClientAuthenticator> authenticators) {
        return new CompositeWsClientAuthenticator(authenticators.stream()
            .sorted(WsClientAuthenticator.COMPARATOR)
            .toList()
        );
    }

    @Override
    public void applyAuthentication(WsClientUpgradeRequest req) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting composite WS client authentication, evaluating {} authenticators",
                authenticators.size());
        }
        for (final WsClientAuthenticator authenticator : authenticators) {
            authenticator.applyAuthentication(req);
            LOG.debug("Authentication apply via: {}", authenticator.getClass().getSimpleName());
        }
    }
}
