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
package xyz.noisif.nsl.http.filter;

import xyz.noisif.nsl.http.HttpRequest;
import xyz.noisif.nsl.http.HttpResponse;
import xyz.noisif.nsl.http.header.TestHttpHeaderName;
import xyz.noisif.nsl.http.route.Route;

import jakarta.inject.Singleton;

@Singleton
public class FirstPriorityFilter implements HttpFilter {
  @Override
  public boolean supports(Route route) {
    return route.path().equals("/api/public");
  }

  @Override
  public boolean preHandle(HttpRequest req, HttpResponse res) {
    res.setHeader(TestHttpHeaderName.X_FILTER_ORDER, "First");
    return true;
  }

  @Override
  public int order() {
    return 100;
  }
}
