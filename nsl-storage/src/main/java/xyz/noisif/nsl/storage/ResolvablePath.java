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
package xyz.noisif.nsl.storage;

import java.util.Arrays;
import java.util.Objects;

public final class ResolvablePath {
  private final StoragePath path;
  private final Object[] args;

  public ResolvablePath(StoragePath path, Object... args) {
    this.path = path;
    this.args = args != null ? args : new Object[0];
  }

  public String buildFromArgs() {
    return path.buildWithArgs(args);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ResolvablePath that = (ResolvablePath) o;
    return Objects.equals(path, that.path) && Arrays.equals(args, that.args);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(path);
    result = 31 * result + Arrays.hashCode(args);
    return result;
  }
}
