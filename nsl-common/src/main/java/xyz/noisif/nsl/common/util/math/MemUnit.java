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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum MemUnit {
  BYTES('\0', 1L),
  KB('K', 1024L),
  MB('M', 1024L * 1024L),
  GB('G', 1024L * 1024L * 1024L),
  ;

  private final char jvmSuffix;
  private final long factor;

  private static final Map<Character, MemUnit> SUFFIX_MAP;

  static {
    final Map<Character, MemUnit> tempMap = new HashMap<>();
    for (final MemUnit unit : values()) {
      tempMap.put(unit.jvmSuffix, unit);
    }
    SUFFIX_MAP = Collections.unmodifiableMap(tempMap);
  }

  MemUnit(char jvmSuffix, long factor) {
    this.jvmSuffix = jvmSuffix;
    this.factor = factor;
  }

  public long toBytes(long value) {
    return value * factor;
  }

  public static MemUnit fromSuffix(char suffix) {
    final MemUnit unit = SUFFIX_MAP.get(suffix);
    if (unit == null) {
      throw new IllegalArgumentException("unsupported memory unit suffix: " + suffix);
    }
    return unit;
  }
}
