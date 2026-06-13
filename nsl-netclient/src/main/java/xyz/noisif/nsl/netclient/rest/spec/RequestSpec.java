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
package xyz.noisif.nsl.netclient.rest.spec;

import xyz.noisif.nsl.codec.serialization.SerializerFormat;
import xyz.noisif.nsl.net.http.auth.AuthScheme;
import xyz.noisif.nsl.net.http.auth.StandardAuthScheme;
import xyz.noisif.nsl.net.http.header.CommonHttpHeaderName;
import xyz.noisif.nsl.net.http.header.HttpHeaderName;
import xyz.noisif.nsl.net.http.header.HttpHeaderValue;
import xyz.noisif.nsl.netclient.group.ClientGroup;
import xyz.noisif.nsl.netclient.rest.RestResponse;
import xyz.noisif.nsl.netclient.rest.intercept.RequestInterceptor;

import java.time.Duration;

public interface RequestSpec {
  RequestSpec group(ClientGroup clientGroup);

  default RequestSpec header(HttpHeaderName name, HttpHeaderValue value, Object... args) {
    return unsafeHeader(name, value.buildWithArgs(args));
  }

  RequestSpec unsafeHeader(HttpHeaderName name, String value);

  default RequestSpec auth(AuthScheme scheme, String... credentials) {
    return unsafeHeader(CommonHttpHeaderName.AUTHORIZATION, scheme.buildHeaderValue(credentials));
  }

  default RequestSpec bearerAuth(String token) {
    return auth(StandardAuthScheme.BEARER, token);
  }

  default RequestSpec basicAuth(String username, String password) {
    return auth(StandardAuthScheme.BASIC, username, password);
  }

  RequestSpec queryParam(String name, String value);

  RequestSpec formParam(String name, String value);

  RequestSpec body(Object body);

  RequestSpec serializer(SerializerFormat format);

  RequestSpec timeout(Duration timeout);

  // maxAttempts = maxRetries + 1
  RequestSpec retry(int maxRetries, Duration backoffMs);

  RequestSpec retry(int maxRetries, Duration backoffMs, Duration maxBackoffMs);

  RequestSpec disableRetry();

  RequestSpec interceptor(RequestInterceptor interceptor);

  <T> RestResponse<T> send(Class<T> responseType);

  default RestResponse<Void> send() {
    return send(Void.class);
  }
}
