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

import xyz.noisif.nsl.net.http.auth.AuthScheme;
import xyz.noisif.nsl.net.http.header.HttpHeaderName;
import xyz.noisif.nsl.net.http.header.HttpHeaderValue;
import xyz.noisif.nsl.netclient.rest.RestResponse;

public interface InterceptorContext {
  RequestView getView();

  default void addHeader(HttpHeaderName name, HttpHeaderValue value, Object... args) {
    addUnsafeHeader(name, value.buildWithArgs(args));
  }

  void addUnsafeHeader(HttpHeaderName name, String value);

  void addQueryParam(String name, String value);

  void setAuth(AuthScheme scheme, String... credentials);

  void abortWith(RestResponse<?> response);
}
