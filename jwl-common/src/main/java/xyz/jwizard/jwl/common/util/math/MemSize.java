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
package xyz.jwizard.jwl.common.util.math;

import xyz.jwizard.jwl.common.bootstrap.ForbiddenInstantiationException;

public class MemSize {
  private MemSize() {
    throw new ForbiddenInstantiationException(MemSize.class);
  }

  public static long of(long size, MemUnit unit) {
    return unit.toBytes(size);
  }

  public static int of(int size, MemUnit unit) {
    return Math.toIntExact(unit.toBytes(size));
  }
}
