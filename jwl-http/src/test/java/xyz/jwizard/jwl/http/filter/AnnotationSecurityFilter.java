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
package xyz.jwizard.jwl.http.filter;

import xyz.jwizard.jwl.http.HttpRequest;
import xyz.jwizard.jwl.http.HttpResponse;
import xyz.jwizard.jwl.http.TestConstants;
import xyz.jwizard.jwl.http.header.TestHttpHeaderName;
import xyz.jwizard.jwl.http.header.TestHttpHeaderValue;
import xyz.jwizard.jwl.net.http.HttpStatus;
import xyz.jwizard.jwl.net.http.header.CommonHttpHeaderName;

import jakarta.inject.Singleton;

@Singleton
public class AnnotationSecurityFilter extends SecureRouteFilter {
  @Override
  public boolean preHandle(HttpRequest req, HttpResponse res) {
    final String token = req.getHeader(CommonHttpHeaderName.AUTHORIZATION);
    if (!TestConstants.TEST_PASSWORD.equals(token)) {
      res.setStatus(HttpStatus.UNAUTHORIZED_401);
      return false;
    }
    res.setHeader(TestHttpHeaderName.X_SECURED_BY, TestHttpHeaderValue.ANNOTATION_FILTER);
    return true;
  }

  @Override
  public int order() {
    return 1000;
  }
}
