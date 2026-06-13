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
package xyz.noisif.nsl.net.http.auth;

import xyz.noisif.nsl.common.util.CodecUtil;

import java.util.function.Function;

public enum StandardAuthScheme implements AuthScheme {
  BLANK("", 1, args -> args[0]),
  BEARER("Bearer", 1, args -> args[0]),
  BASIC("Basic", 2, args -> CodecUtil.encodeBase64(args[0] + ":" + args[1]));

  private final String schemeName;
  private final int requiredParams;
  private final Function<String[], String> formatter;

  StandardAuthScheme(String schemeName, int requiredParams, Function<String[], String> formatter) {
    this.schemeName = schemeName;
    this.requiredParams = requiredParams;
    this.formatter = formatter;
  }

  @Override
  public String buildHeaderValue(String... credentials) {
    final int providedCount = (credentials == null) ? 0 : credentials.length;
    if (providedCount != requiredParams) {
      throw new IllegalArgumentException(
          String.format(
              "%s auth requires exactly %d parameter(s), but got %d",
              schemeName, requiredParams, providedCount));
    }
    final String formattedCredentials = formatter.apply(credentials);
    return schemeName + (schemeName.isBlank() ? "" : " ") + formattedCredentials;
  }
}
