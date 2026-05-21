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
