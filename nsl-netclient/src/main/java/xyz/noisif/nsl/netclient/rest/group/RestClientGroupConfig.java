/*
 * Copyright (c) 2022-2026 NOISIF. All Rights Reserved.
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
package xyz.noisif.nsl.netclient.rest.group;

import xyz.noisif.nsl.codec.serialization.SerializerFormat;
import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;
import xyz.noisif.nsl.common.Ordered;
import xyz.noisif.nsl.common.limit.RateLimiter;
import xyz.noisif.nsl.net.http.HttpMethod;
import xyz.noisif.nsl.net.http.auth.AuthScheme;
import xyz.noisif.nsl.netclient.group.GenericClientGroupConfig;
import xyz.noisif.nsl.netclient.rest.intercept.AuthInterceptor;
import xyz.noisif.nsl.netclient.rest.intercept.RateLimitInterceptor;
import xyz.noisif.nsl.netclient.rest.intercept.RequestInterceptor;
import xyz.noisif.nsl.netclient.rest.intercept.UserAgentInterceptor;
import xyz.noisif.nsl.netclient.rest.retry.RetryPolicy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

    private Builder() {}

    @Override
    protected Builder self() {
      return this;
    }

    public Builder defaultFormat(SerializerFormat defaultFormat) {
      this.defaultFormat = defaultFormat;
      return self();
    }

    public Builder retryOnSafeMethods(int maxRetries, Duration backoff) {
      retryPolicy = RetryPolicy.withSafeMethods(maxRetries + 1, backoff);
      return self();
    }

    public Builder retryOnSafeMethods(int maxRetries, Duration backoff, Duration maxBackoff) {
      retryPolicy = RetryPolicy.withSafeMethods(maxRetries + 1, backoff, maxBackoff);
      return self();
    }

    public Builder retry(
        int maxRetries, Duration backoff, Duration maxBackoff, HttpMethod... methods) {
      retryPolicy = RetryPolicy.with(maxRetries + 1, backoff, maxBackoff, methods);
      return self();
    }

    public Builder retry(int maxRetries, Duration backoff, HttpMethod... methods) {
      retryPolicy = RetryPolicy.with(maxRetries + 1, backoff, methods);
      return self();
    }

    public Builder disableRetry() {
      retryPolicy = RetryPolicy.none();
      return self();
    }

    public Builder auth(AuthScheme scheme, String... credentials) {
      return auth(Integer.MAX_VALUE, scheme, credentials);
    }

    public Builder auth(int order, AuthScheme scheme, String... credentials) {
      interceptors.add(new AuthInterceptor(order, scheme, credentials));
      return self();
    }

    public Builder rateLimit(RateLimiter rateLimiter) {
      interceptors.add(new RateLimitInterceptor(rateLimiter));
      return self();
    }

    public Builder interceptor(RequestInterceptor interceptor) {
      interceptors.add(interceptor);
      return self();
    }

    @Override
    public Builder principalId(String principalId) {
      super.principalId(principalId);
      interceptors.add(new UserAgentInterceptor(principalId));
      return self();
    }

    @Override
    public RestClientGroupConfig build() {
      super.validate();
      interceptors.sort(Ordered.COMPARATOR);
      return new RestClientGroupConfig(this);
    }
  }
}
