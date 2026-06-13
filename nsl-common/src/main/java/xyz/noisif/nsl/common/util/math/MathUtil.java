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
package xyz.noisif.nsl.common.util.math;

import xyz.noisif.nsl.common.bootstrap.ForbiddenInstantiationException;

public class MathUtil {
  private MathUtil() {
    throw new ForbiddenInstantiationException(MathUtil.class);
  }

  public static long calcExpBackoff(int attempt, long baseDelayMs, long maxDelayMs) {
    if (baseDelayMs <= 0) {
      return 0;
    }
    final long expDelay = baseDelayMs * (long) Math.pow(2, Math.max(0, attempt - 1));
    final double jitterFactor = 1.0 + (Math.random() * 0.2 - 0.1);
    final long finalDelay = (long) (expDelay * jitterFactor);
    return Math.min(finalDelay, maxDelayMs);
  }
}
