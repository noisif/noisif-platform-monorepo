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

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.net.http.header.CommonHttpHeaderName;
import xyz.jwizard.jwl.websocket.WsHandshakeRequest;

public class WsTokenAuthenticator implements WsAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(WsTokenAuthenticator.class);

    private final String expectedToken;
    private final String principalId;
    private final String queryParameterKey;

    private WsTokenAuthenticator(Builder builder) {
        expectedToken = builder.expectedToken;
        principalId = builder.principalId;
        queryParameterKey = builder.queryParameterKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String authenticate(WsHandshakeRequest req) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Attempting token authentication for configured principal: '{}'",
                principalId);
        }
        final String incomingToken = req.getHeader(CommonHttpHeaderName.AUTHORIZATION);
        if (expectedToken.equals(incomingToken)) {
            LOG.trace("Authentication successful via HTTP header for principal: '{}'", principalId);
            return principalId;
        }
        if (queryParameterKey == null) {
            LOG.debug("Authentication failed for principal: '{}', " +
                "header token mismatch and query parameter fallback is disabled", principalId);
            return null;
        }
        LOG.trace("Header token mismatch. Falling back to query parameter check using key: '?{}='",
            queryParameterKey);
        final List<String> tokenParams = req.getQueryParameter(queryParameterKey);
        if (tokenParams == null || tokenParams.isEmpty()) {
            LOG.debug("Authentication failed for principal: '{}', query parameter '{}' is missing",
                principalId, queryParameterKey);
            return null;
        }
        if (expectedToken.equals(tokenParams.getFirst())) {
            LOG.trace("Authentication successful via query parameter '{}' for principal: '{}'",
                queryParameterKey, principalId);
            return principalId;
        }
        LOG.debug("Authentication failed for principal: '{}', query parameter token mismatch",
            principalId);
        return null;
    }

    public static class Builder {
        private String expectedToken;
        private String principalId;
        private String queryParameterKey = null;

        private Builder() {
        }

        public Builder expectedToken(String expectedToken) {
            this.expectedToken = expectedToken;
            return this;
        }

        public Builder principalId(String principalId) {
            this.principalId = principalId;
            return this;
        }

        public Builder withQueryParameterCheck(@Nullable String queryParameterKey) {
            this.queryParameterKey = queryParameterKey;
            return this;
        }

        public WsTokenAuthenticator build() {
            Assert.notNull(expectedToken, "ExpectedToken cannot be null");
            Assert.notNull(principalId, "PrincipalId cannot be null");
            return new WsTokenAuthenticator(this);
        }
    }
}
