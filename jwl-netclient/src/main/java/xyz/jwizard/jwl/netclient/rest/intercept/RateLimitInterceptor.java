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
package xyz.jwizard.jwl.netclient.rest.intercept;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.limit.RateLimiter;
import xyz.jwizard.jwl.net.http.HttpStatus;
import xyz.jwizard.jwl.netclient.rest.RestResponse;

import java.util.Map;

public class RateLimitInterceptor implements RequestInterceptor {
  private static final Logger LOG = LoggerFactory.getLogger(RateLimitInterceptor.class);

  private final RateLimiter rateLimiter;
  private final int order;

  public RateLimitInterceptor(RateLimiter rateLimiter, int order) {
    this.rateLimiter = rateLimiter;
    this.order = order;
  }

  public RateLimitInterceptor(RateLimiter rateLimiter) {
    this(rateLimiter, Integer.MAX_VALUE);
  }

  @Override
  public void intercept(InterceptorContext context) {
    final RequestView view = context.getView();
    final String groupName = view.getGroup().getClientGroupName();

    LOG.trace(
        "Intercepting request for group: {} (method: {}, path: {})",
        groupName,
        view.getMethod(),
        view.getUrl());

    if (!rateLimiter.tryAcquire(groupName)) {
      LOG.info(
          "Rate limit exceeded for group: {}, aborting request: {} {}",
          groupName,
          view.getMethod(),
          view.getUrl());
      final RestResponse<Void> tooManyRequestsResponse =
          new RestResponse<>(HttpStatus.TOO_MANY_REQUESTS_429.getCode(), Map.of(), null);
      context.abortWith(tooManyRequestsResponse);
    }
    LOG.debug("Rate limit permit acquired for group: {}", groupName);
  }

  @Override
  public int order() {
    return order;
  }
}
