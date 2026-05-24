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
