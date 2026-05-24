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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.netclient.websocket.WsClientUpgradeRequest;

public class WsQueryParamAuthenticator implements WsClientAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(WsQueryParamAuthenticator.class);

    private final String queryParameterKey;
    private final String queryParameterValue;

    private WsQueryParamAuthenticator(Builder builder) {
        queryParameterKey = builder.queryParameterKey;
        queryParameterValue = builder.queryParameterValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void applyAuthentication(WsClientUpgradeRequest req) {
        LOG.trace("Applying authentication via query parameter: '?{}='", queryParameterKey);
        req.addQueryParameter(queryParameterKey, queryParameterValue);
    }

    public static class Builder {
        private String queryParameterKey = "token";
        private String queryParameterValue = null;

        private Builder() {
        }

        public Builder queryParameterKey(String queryParameterKey) {
            this.queryParameterKey = queryParameterKey;
            return this;
        }

        public Builder queryParameterValue(String queryParameterValue) {
            this.queryParameterValue = queryParameterValue;
            return this;
        }

        public WsQueryParamAuthenticator build() {
            Assert.notNull(queryParameterKey, "QueryParameterKey cannot be null");
            Assert.notNull(queryParameterValue, "QueryParameterValue cannot be null");
            return new WsQueryParamAuthenticator(this);
        }
    }
}
