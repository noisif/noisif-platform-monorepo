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

import xyz.noisif.nsl.netclient.rest.TestHttpHeaderName;
import xyz.noisif.nsl.netclient.rest.TestHttpHeaderValue;

public class CorrelationInterceptor implements RequestInterceptor {
  private final String correlationId;
  private final String correlationCode;

  public CorrelationInterceptor(String correlationId, String correlationCode) {
    this.correlationId = correlationId;
    this.correlationCode = correlationCode;
  }

  @Override
  public void intercept(InterceptorContext context) {
    context.addHeader(
        TestHttpHeaderName.X_CORRELATION_ID,
        TestHttpHeaderValue.REQ,
        correlationId,
        correlationCode);
    context.addQueryParam("tracking_enabled", "true");
  }

  @Override
  public int order() {
    return 10;
  }
}
