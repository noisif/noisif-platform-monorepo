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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.net.http.auth.AuthScheme;
import xyz.jwizard.jwl.net.http.header.CommonHttpHeaderName;
import xyz.jwizard.jwl.net.http.header.HttpHeaderName;
import xyz.jwizard.jwl.netclient.websocket.WsClientUpgradeRequest;

public class WsClientHeaderAuthenticator implements WsClientAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(WsClientHeaderAuthenticator.class);

    private final HttpHeaderName headerName;
    private final AuthScheme authScheme;
    private final String[] credentials;

    private WsClientHeaderAuthenticator(Builder builder) {
        headerName = builder.headerName;
        authScheme = builder.authScheme;
        credentials = builder.credentials;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void applyAuthentication(WsClientUpgradeRequest req) {
        final String headerValue = authScheme.buildHeaderValue(credentials);
        LOG.trace("Applying authentication via HTTP Authorization header with scheme: {}",
            authScheme);
        req.setHeader(headerName, headerValue);
    }

    public static class Builder {
        private HttpHeaderName headerName = CommonHttpHeaderName.AUTHORIZATION;
        private AuthScheme authScheme;
        private String[] credentials;

        private Builder() {
        }

        public Builder headerName(HttpHeaderName headerName) {
            this.headerName = headerName;
            return this;
        }

        public Builder authScheme(AuthScheme authScheme) {
            this.authScheme = authScheme;
            return this;
        }

        public Builder credentials(String... credentials) {
            this.credentials = credentials;
            return this;
        }

        public WsClientHeaderAuthenticator build() {
            Assert.notNull(headerName, "HeaderName cannot be null");
            Assert.notNull(authScheme, "AuthScheme cannot be null");
            return new WsClientHeaderAuthenticator(this);
        }
    }
}
