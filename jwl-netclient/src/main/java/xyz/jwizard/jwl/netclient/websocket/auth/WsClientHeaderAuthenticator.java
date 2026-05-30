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
    LOG.trace("Applying authentication via HTTP Authorization header with scheme: {}", authScheme);
    req.setHeader(headerName, headerValue);
  }

  public static class Builder {
    private HttpHeaderName headerName = CommonHttpHeaderName.AUTHORIZATION;
    private AuthScheme authScheme;
    private String[] credentials;

    private Builder() {}

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
