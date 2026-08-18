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
package xyz.noisif.nsl.zcache;

import xyz.noisif.nsl.common.bootstrap.lifecycle.IdempotentService;
import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.common.util.math.MemSize;
import xyz.noisif.nsl.common.util.math.MemUnit;
import xyz.noisif.nsl.common.util.system.DefaultJvmArg;
import xyz.noisif.nsl.common.util.system.JvmRuntimeUtil;

public abstract class GenericNativeStorage extends IdempotentService implements NativeStorage {
  protected final long maxBytes;
  protected final int initialCapacity;
  protected final String memorySource;

  protected GenericNativeStorage(AbstractBuilder<?> builder) {
    maxBytes = builder.maxBytes;
    initialCapacity = builder.initialCapacity;
    memorySource = builder.memorySource;
  }

  @Override
  protected void onStart() {
    log.debug(
        "starting zcache native storage, capacity: {} bytes (source: {}), initial map size: {}",
        maxBytes,
        memorySource,
        initialCapacity);
  }

  @Override
  protected void onStop() {
    log.info("stopping native storage zcache, shattering all remaining off-heap allocations");
    empty();
  }

  public abstract static class AbstractBuilder<B extends AbstractBuilder<B>> {
    protected long maxBytes = -1;
    protected int initialCapacity = -1;
    protected String memorySource = "unknown";

    protected AbstractBuilder() {}

    protected abstract B self();

    public B maxMemory(long maxMemory, MemUnit memUnit) {
      maxBytes = MemSize.of(maxMemory, memUnit);
      memorySource = "explicitly configured (" + maxMemory + " " + memUnit.name() + ")";
      return self();
    }

    public B useJvmMaxDirectMemory() {
      maxBytes = JvmRuntimeUtil.getJvmArg(DefaultJvmArg.MAX_DIRECT_MEMORY_SIZE);
      memorySource = "JVM flag (" + DefaultJvmArg.MAX_DIRECT_MEMORY_SIZE.getPrefix() + ")";
      return self();
    }

    public B useJvmMaxDirectMemory(double usageFraction) {
      final long totalDirect = JvmRuntimeUtil.getJvmArg(DefaultJvmArg.MAX_DIRECT_MEMORY_SIZE);
      maxBytes = (long) (totalDirect * usageFraction);
      memorySource =
          String.format("JVM flag (-XX:MaxDirectMemorySize, fraction: %.2f)", usageFraction);
      return self();
    }

    public B initialCapacity(int initialCapacity) {
      this.initialCapacity = initialCapacity;
      return self();
    }

    protected void validate() {
      Assert.greaterThan(maxBytes, -1, "maxBytes");
      Assert.greaterThan(initialCapacity, -1, "initialCapacity");
    }

    public abstract NativeStorage build();
  }
}
