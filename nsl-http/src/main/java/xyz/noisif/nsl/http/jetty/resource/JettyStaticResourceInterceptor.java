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
package xyz.noisif.nsl.http.jetty.resource;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import xyz.noisif.nsl.http.Interceptor;
import xyz.noisif.nsl.http.jetty.adapter.JettyHttpRequestAdapter;
import xyz.noisif.nsl.http.jetty.adapter.JettyHttpResponseAdapter;
import xyz.noisif.nsl.http.resource.StaticResourceDecision;

public class JettyStaticResourceInterceptor extends Handler.Wrapper {
  private final Interceptor<StaticResourceDecision> interceptor;

  public JettyStaticResourceInterceptor(
      Handler wrapped, Interceptor<StaticResourceDecision> interceptor) {
    super(wrapped);
    this.interceptor = interceptor;
  }

  @Override
  public boolean handle(Request request, Response response, Callback callback) throws Exception {
    final JettyHttpRequestAdapter reqAdapter = new JettyHttpRequestAdapter(request);
    final JettyHttpResponseAdapter resAdapter = new JettyHttpResponseAdapter(response, callback);
    final StaticResourceDecision decision = interceptor.intercept(reqAdapter, resAdapter);
    return switch (decision) {
      case PASS_TO_API -> false;
      case HANDLED_BY_FALLBACK -> true;
      case CONTINUE_WITH_DEFAULT_HANDLER -> super.handle(request, response, callback);
    };
  }
}
