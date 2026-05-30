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
package xyz.jwizard.jwl.netclient.rest.jetty.spec;

import org.eclipse.jetty.client.Request;

import xyz.jwizard.jwl.net.http.header.HttpHeaderName;
import xyz.jwizard.jwl.netclient.rest.spec.HeaderConsumer;

public class JettyHeaderConsumer implements HeaderConsumer {
  private final Request request;

  public JettyHeaderConsumer(Request request) {
    this.request = request;
  }

  @Override
  public void addHeader(HttpHeaderName name, String value) {
    request.headers(h -> h.put(name.getCode(), value));
  }
}
