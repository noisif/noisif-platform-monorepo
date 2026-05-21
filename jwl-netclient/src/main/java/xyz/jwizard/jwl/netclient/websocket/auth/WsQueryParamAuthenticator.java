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
