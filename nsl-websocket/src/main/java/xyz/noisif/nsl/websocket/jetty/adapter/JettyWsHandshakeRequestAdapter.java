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
package xyz.noisif.nsl.websocket.jetty.adapter;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;

import xyz.noisif.nsl.net.http.cookie.CookieName;
import xyz.noisif.nsl.net.http.header.HttpHeaderName;
import xyz.noisif.nsl.websocket.WsHandshakeRequest;

import java.util.List;

public class JettyWsHandshakeRequestAdapter implements WsHandshakeRequest {
  private final ServerUpgradeRequest req;

  public JettyWsHandshakeRequestAdapter(ServerUpgradeRequest req) {
    this.req = req;
  }

  @Override
  public String getHeader(HttpHeaderName header) {
    return req.getHeaders().get(header.getCode());
  }

  @Override
  public String getCookie(CookieName name) {
    final List<HttpCookie> cookies = Request.getCookies(req);
    if (cookies == null) {
      return null;
    }
    for (final HttpCookie cookie : cookies) {
      if (cookie.getName().equals(name.getCode())) {
        return cookie.getValue();
      }
    }
    return null;
  }

  @Override
  public List<String> getQueryParameter(String key) {
    final Fields queryParams = Request.extractQueryParameters(req);
    final List<String> values = queryParams.getValues(key);
    return values != null ? values : List.of();
  }
}
