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
package xyz.noisif.nsl.common.util;

import xyz.noisif.nsl.common.bootstrap.CriticalBootstrapException;
import xyz.noisif.nsl.common.bootstrap.ForbiddenInstantiationException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtil {
  private CryptoUtil() {
    throw new ForbiddenInstantiationException(CryptoUtil.class);
  }

  public static String calculateSha1(String text) {
    if (text == null) {
      return null;
    }
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-1");
      final byte[] bytes = md.digest(StringUtil.getBytes(text));
      final StringBuilder stringBuilder = new StringBuilder();
      for (final byte b : bytes) {
        stringBuilder.append(String.format("%02x", b));
      }
      return stringBuilder.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new CriticalBootstrapException("SHA-1 algorithm not found", ex);
    }
  }
}
