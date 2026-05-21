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
package xyz.jwizard.jwl.netclient.rest.group;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import xyz.jwizard.jwl.codec.serialization.SerializerFormat;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;
import xyz.jwizard.jwl.common.Ordered;
import xyz.jwizard.jwl.common.limit.RateLimiter;
import xyz.jwizard.jwl.net.http.HttpMethod;
import xyz.jwizard.jwl.net.http.auth.AuthScheme;
import xyz.jwizard.jwl.netclient.group.GenericClientGroupConfig;
import xyz.jwizard.jwl.netclient.rest.intercept.AuthInterceptor;
import xyz.jwizard.jwl.netclient.rest.intercept.RateLimitInterceptor;
import xyz.jwizard.jwl.netclient.rest.intercept.RequestInterceptor;
import xyz.jwizard.jwl.netclient.rest.intercept.UserAgentInterceptor;
import xyz.jwizard.jwl.netclient.rest.retry.RetryPolicy;

public class RestClientGroupConfig extends GenericClientGroupConfig {
    private final SerializerFormat defaultFormat;
    private final RetryPolicy retryPolicy;
    private final List<RequestInterceptor> interceptors;

    private RestClientGroupConfig(Builder builder) {
        super(builder);
        defaultFormat = builder.defaultFormat;
        retryPolicy = builder.retryPolicy;
        interceptors = builder.interceptors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public SerializerFormat getDefaultFormat() {
        return defaultFormat;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public List<RequestInterceptor> getInterceptors() {
        return interceptors;
    }

    public static class Builder extends AbstractBuilder<Builder, RestClientGroupConfig> {
        private final List<RequestInterceptor> interceptors = new ArrayList<>();
        private SerializerFormat defaultFormat = StandardSerializerFormat.JSON;
        private RetryPolicy retryPolicy = RetryPolicy.none();

        private Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder defaultFormat(SerializerFormat defaultFormat) {
            this.defaultFormat = defaultFormat;
            return this;
        }

        public Builder retryOnSafeMethods(int maxRetries, Duration backoff) {
            retryPolicy = RetryPolicy.withSafeMethods(maxRetries + 1, backoff);
            return this;
        }

        public Builder retryOnSafeMethods(int maxRetries, Duration backoff, Duration maxBackoff) {
            retryPolicy = RetryPolicy.withSafeMethods(maxRetries + 1, backoff, maxBackoff);
            return this;
        }

        public Builder retry(int maxRetries, Duration backoff, Duration maxBackoff,
                             HttpMethod... methods) {
            retryPolicy = RetryPolicy.with(maxRetries + 1, backoff, maxBackoff, methods);
            return this;
        }

        public Builder retry(int maxRetries, Duration backoff, HttpMethod... methods) {
            retryPolicy = RetryPolicy.with(maxRetries + 1, backoff, methods);
            return this;
        }

        public Builder disableRetry() {
            retryPolicy = RetryPolicy.none();
            return this;
        }

        public Builder auth(AuthScheme scheme, String... credentials) {
            return auth(Integer.MAX_VALUE, scheme, credentials);
        }

        public Builder auth(int order, AuthScheme scheme, String... credentials) {
            interceptors.add(new AuthInterceptor(order, scheme, credentials));
            return this;
        }

        public Builder rateLimit(RateLimiter rateLimiter) {
            interceptors.add(new RateLimitInterceptor(rateLimiter));
            return this;
        }

        public Builder interceptor(RequestInterceptor interceptor) {
            interceptors.add(interceptor);
            return this;
        }

        @Override
        public Builder principalId(String principalId) {
            super.principalId(principalId);
            interceptors.add(new UserAgentInterceptor(principalId));
            return this;
        }

        @Override
        public RestClientGroupConfig build() {
            super.validate();
            interceptors.sort(Ordered.COMPARATOR);
            return new RestClientGroupConfig(this);
        }
    }
}
