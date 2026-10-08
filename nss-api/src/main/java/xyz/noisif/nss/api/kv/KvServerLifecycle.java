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
package xyz.noisif.nss.api.kv;

import xyz.noisif.nsl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.reflect.ClassScanner;
import xyz.noisif.nsl.kv.KvServer;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class KvServerLifecycle implements LifecycleHook {
  private final KvServer kvServer;

  @Inject
  KvServerLifecycle(KvServer kvServer) {
    this.kvServer = kvServer;
  }

  @Override
  public void onStart(ComponentProvider componentProvider, ClassScanner classScanner) {
    kvServer.start();
  }

  @Override
  public void onStop() {
    kvServer.close();
  }
}
