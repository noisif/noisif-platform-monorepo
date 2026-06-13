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
package xyz.noisif.nsl.http.writer;

import xyz.noisif.nsl.codec.serialization.json.JsonSerializer;
import xyz.noisif.nsl.http.HttpResponse;
import xyz.noisif.nsl.net.http.header.CommonHttpHeaderName;
import xyz.noisif.nsl.net.http.header.CommonHttpHeaderValue;

public class JsonResponseWriter implements ResponseWriter {
  private final JsonSerializer jsonSerializer;

  public JsonResponseWriter(JsonSerializer jsonSerializer) {
    this.jsonSerializer = jsonSerializer;
  }

  @Override
  public boolean supports(Object result) {
    return result != null; // supports all which is not string and null
  }

  @Override
  public void write(HttpResponse res, Object result) {
    res.setHeader(CommonHttpHeaderName.CONTENT_TYPE, CommonHttpHeaderValue.APPLICATION_JSON_UTF_8);
    final String json = jsonSerializer.serialize(result);
    res.write(json, true);
  }
}
