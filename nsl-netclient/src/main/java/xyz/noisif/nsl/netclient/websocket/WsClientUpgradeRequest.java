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
package xyz.noisif.nsl.netclient.websocket;

import xyz.noisif.nsl.net.NetworkUtil;
import xyz.noisif.nsl.net.http.header.HttpHeaderName;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WsClientUpgradeRequest {
  private final Map<String, List<String>> headers = new HashMap<>();
  private String uri;

  public WsClientUpgradeRequest(URI uri) {
    this.uri = uri.toASCIIString();
  }

  public void setHeader(HttpHeaderName name, List<String> value) {
    headers.put(name.getCode(), value);
  }

  public void setHeader(HttpHeaderName name, String value) {
    setHeader(name, List.of(value));
  }

  public void addQueryParameter(String key, String value) {
    uri = NetworkUtil.addQueryParameter(uri, key, value);
  }

  public URI getUri() {
    return URI.create(uri);
  }

  public Map<String, List<String>> getHeaders() {
    return headers;
  }
}
