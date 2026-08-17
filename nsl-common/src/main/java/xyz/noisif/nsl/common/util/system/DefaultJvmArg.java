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
package xyz.noisif.nsl.common.util.system;

import xyz.noisif.nsl.common.util.math.MemSize;

import java.util.function.Function;

public enum DefaultJvmArg implements JvmArg {
  MAX_DIRECT_MEMORY_SIZE("-XX:MaxDirectMemorySize=", MemSize::parseFromStr, Long.class),
  ;

  private final String prefix;
  private final Function<String, ?> transformer;
  private final Class<?> type;

  <T> DefaultJvmArg(String prefix, Function<String, T> transformer, Class<T> type) {
    this.prefix = prefix;
    this.transformer = transformer;
    this.type = type;
  }

  @Override
  public String getPrefix() {
    return prefix;
  }

  @Override
  public Function<String, ?> getTransformer() {
    return transformer;
  }

  @Override
  public Class<?> getType() {
    return type;
  }
}
