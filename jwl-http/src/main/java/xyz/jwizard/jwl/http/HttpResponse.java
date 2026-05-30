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
package xyz.jwizard.jwl.http;

import xyz.jwizard.jwl.net.http.HttpStatus;
import xyz.jwizard.jwl.net.http.header.HttpHeaderName;
import xyz.jwizard.jwl.net.http.header.HttpHeaderValue;

public interface HttpResponse {
  String getHeaderUnsafe(String name);

  String getHeader(HttpHeaderName name);

  void setStatus(HttpStatus statusCode);

  void setHeader(HttpHeaderName name, HttpHeaderValue value, Object... args);

  void setHeader(HttpHeaderName name, String value);

  void setHeaderUnsafe(String name, String value);

  void write(String body, boolean last);

  void writeEmpty(boolean last);

  void end();
}
