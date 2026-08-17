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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.bootstrap.ForbiddenInstantiationException;
import xyz.noisif.nsl.common.util.StringUtil;

public class MemSize {
  private static final Logger LOG = LoggerFactory.getLogger(MemSize.class);

  private MemSize() {
    throw new ForbiddenInstantiationException(MemSize.class);
  }

  public static long of(long size, MemUnit unit) {
    return unit.toBytes(size);
  }

  public static int of(int size, MemUnit unit) {
    return Math.toIntExact(unit.toBytes(size));
  }

  public static long parseFromStr(String sizeString) {
    if (sizeString == null || sizeString.isBlank()) {
      LOG.trace("empty memory size string provided, returning 0");
      return 0L;
    }
    final String upper = StringUtil.toUpperCase(sizeString.trim());
    final char lastChar = upper.charAt(upper.length() - 1);
    if (Character.isDigit(lastChar)) {
      LOG.trace("no memory unit suffix detected, assuming raw bytes");
      return Long.parseLong(upper);
    }
    // chop off the letter and parse the raw number
    final String numberPart = upper.substring(0, upper.length() - 1);
    final long parsedValue = Long.parseLong(numberPart);
    // let the enum figure out what the suffix means
    final MemUnit unit = MemUnit.fromSuffix(lastChar);
    LOG.trace("detected {} memory unit from suffix '{}'", unit.name(), lastChar);
    return MemSize.of(parsedValue, unit);
  }
}
