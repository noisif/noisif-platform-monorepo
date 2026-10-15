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
package xyz.noisif.nsl.ui.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class HttpRequest {
  private final HttpMethod method;
  private final String url;
  private final Map<String, String> headers;
  private final JsonPayload body;
  private final boolean includeCredentials;
  private final int timeoutMs;

  private HttpRequest(Builder builder) {
    method = builder.method;
    url = builder.url;
    headers = new HashMap<>(builder.headers);
    body = builder.body;
    includeCredentials = builder.includeCredentials;
    timeoutMs = builder.timeoutMs;
  }

  public HttpMethod method() {
    return method;
  }

  public String url() {
    return url;
  }

  public Map<String, String> headers() {
    return Collections.unmodifiableMap(headers);
  }

  public JsonPayload body() {
    return body;
  }

  public boolean includeCredentials() {
    return includeCredentials;
  }

  public int timeoutMs() {
    return timeoutMs;
  }

  public static Builder get(String url) {
    return new Builder(HttpMethod.GET, url);
  }

  public static Builder post(String url) {
    return new Builder(HttpMethod.POST, url);
  }

  public static Builder put(String url) {
    return new Builder(HttpMethod.PUT, url);
  }

  public static Builder delete(String url) {
    return new Builder(HttpMethod.DELETE, url);
  }

  public static final class Builder {
    private final HttpMethod method;
    private final String url;
    private final Map<String, String> headers = new HashMap<>();
    private JsonPayload body;
    private boolean includeCredentials = true;
    private int timeoutMs = 3000;

    public Builder(HttpMethod method, String url) {
      this.method = method;
      this.url = url;
      headers.put("Accept", "application/json");
    }

    public Builder header(String name, String value) {
      if (value != null) {
        headers.put(name, value);
      }
      return this;
    }

    public Builder jsonBody(JsonPayload payload) {
      if (payload != null) {
        headers.put("Content-Type", "application/json");
        body = payload;
      }
      return this;
    }

    public Builder withCredentials(boolean include) {
      includeCredentials = include;
      return this;
    }

    public Builder timeout(int ms) {
      this.timeoutMs = ms;
      return this;
    }

    public HttpRequest build() {
      return new HttpRequest(this);
    }
  }
}
