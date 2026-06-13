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
package xyz.noisif.nsl.netclient.rest.intercept;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.net.http.auth.AuthScheme;
import xyz.noisif.nsl.net.http.header.CommonHttpHeaderName;

public class AuthInterceptor implements RequestInterceptor {
  private static final Logger LOG = LoggerFactory.getLogger(AuthInterceptor.class);

  private final int order;
  private final AuthScheme scheme;
  private final String[] credentials;

  public AuthInterceptor(int order, AuthScheme scheme, String[] credentials) {
    this.order = order;
    this.scheme = scheme;
    this.credentials = credentials;
  }

  @Override
  public void intercept(InterceptorContext context) {
    final String authHeaderName = CommonHttpHeaderName.AUTHORIZATION.getCode();
    final boolean hasCustomAuth = context.getView().getHeaders().containsKey(authHeaderName);
    if (!hasCustomAuth) {
      LOG.trace("Applying group auth scheme: {}", scheme);
      context.setAuth(scheme, credentials);
    } else {
      LOG.trace("Skipping group auth, request already has custom authorization.");
    }
  }

  @Override
  public int order() {
    return order;
  }
}
