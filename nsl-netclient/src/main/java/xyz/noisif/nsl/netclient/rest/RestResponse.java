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
package xyz.noisif.nsl.netclient.rest;

import xyz.noisif.nsl.net.http.HttpStatus;

import java.util.List;
import java.util.Map;

public class RestResponse<T> {
  private final HttpStatus status;
  private final Map<String, List<String>> headers;
  private final T body;

  public RestResponse(int status, Map<String, List<String>> headers, T body) {
    this.status = HttpStatus.fromCode(status);
    this.headers = headers != null ? headers : Map.of();
    this.body = body;
  }

  public boolean is(HttpStatus status) {
    return this.status.equals(status);
  }

  public HttpStatus getStatus() {
    return status;
  }

  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  public T getBody() {
    return body;
  }
}
