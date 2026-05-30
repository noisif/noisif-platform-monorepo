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
package xyz.jwizard.jwl.netclient.rest.retry;

import xyz.jwizard.jwl.common.retry.RetryPolicyContext;
import xyz.jwizard.jwl.net.http.HttpMethod;

import java.time.Duration;
import java.util.Set;

public class RetryPolicy implements RetryPolicyContext<HttpMethod> {
  private static final long MAX_BACKOFF_MS = 30_000L;
  private static final Set<HttpMethod> SAFE_METHODS =
      Set.of(
          HttpMethod.GET,
          HttpMethod.HEAD,
          HttpMethod.OPTIONS,
          HttpMethod.TRACE,
          HttpMethod.PUT,
          HttpMethod.DELETE);

  private final int maxAttempts;
  private final long backoffMs;
  private final long maxBackoffMs;
  private final Set<HttpMethod> allowedMethods;

  private RetryPolicy(
      int maxAttempts, long backoffMs, long maxBackoffMs, Set<HttpMethod> allowedMethods) {
    this.maxAttempts = maxAttempts;
    this.backoffMs = backoffMs;
    this.maxBackoffMs = maxBackoffMs;
    this.allowedMethods = allowedMethods;
  }

  public static RetryPolicy withSafeMethods(
      int maxAttempts, Duration backoff, Duration maxBackoff) {
    return new RetryPolicy(maxAttempts, backoff.toMillis(), maxBackoff.toMillis(), SAFE_METHODS);
  }

  public static RetryPolicy withSafeMethods(int maxAttempts, Duration backoff) {
    return new RetryPolicy(maxAttempts, backoff.toMillis(), MAX_BACKOFF_MS, SAFE_METHODS);
  }

  public static RetryPolicy with(
      int maxAttempts, Duration backoff, Duration maxBackoff, HttpMethod... allowedMethods) {
    return new RetryPolicy(
        maxAttempts, backoff.toMillis(), maxBackoff.toMillis(), Set.of(allowedMethods));
  }

  public static RetryPolicy with(int maxAttempts, Duration backoff, HttpMethod... allowedMethods) {
    return new RetryPolicy(maxAttempts, backoff.toMillis(), MAX_BACKOFF_MS, Set.of(allowedMethods));
  }

  public static RetryPolicy none() {
    return new RetryPolicy(0, 0, 0, Set.of());
  }

  @Override
  public boolean shouldRetry(int currentAttempt, HttpMethod method) {
    return currentAttempt < maxAttempts && allowedMethods.contains(method);
  }

  @Override
  public boolean isEnabled() {
    return maxAttempts > 0;
  }

  @Override
  public long getBackoffMs() {
    return backoffMs;
  }

  @Override
  public long getMaxBackoffMs() {
    return maxBackoffMs;
  }
}
