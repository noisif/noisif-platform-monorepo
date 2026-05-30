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
package xyz.jwizard.jwl.common.util;

import xyz.jwizard.jwl.common.bootstrap.ForbiddenInstantiationException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CodecUtil {
  private CodecUtil() {
    throw new ForbiddenInstantiationException(CodecUtil.class);
  }

  public static String encodeBase64(String text) {
    if (text == null) {
      return null;
    }
    return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
  }

  public static String decodeBase64(String base64Text) {
    if (base64Text == null) {
      return null;
    }
    final byte[] decodedBytes = Base64.getDecoder().decode(base64Text);
    return new String(decodedBytes, StandardCharsets.UTF_8);
  }
}
