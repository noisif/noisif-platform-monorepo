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
package xyz.noisif.nss.api.sql;

import xyz.noisif.nsl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.reflect.ClassScanner;
import xyz.noisif.nsl.sql.registry.SqlDatabaseRegistry;
import xyz.noisif.nss.api.kv.KvServerLifecycle;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class SqlClientLifecycle implements LifecycleHook {
  private final SqlDatabaseRegistry sqlDatabaseRegistry;

  @Inject
  SqlClientLifecycle(SqlDatabaseRegistry sqlDatabaseRegistry) {
    this.sqlDatabaseRegistry = sqlDatabaseRegistry;
  }

  @Override
  public List<Class<? extends LifecycleHook>> dependsOn() {
    return List.of(KvServerLifecycle.class);
  }

  @Override
  public void onStart(ComponentProvider componentProvider, ClassScanner classScanner) {
    sqlDatabaseRegistry.startAll();
  }

  @Override
  public void onStop() {
    sqlDatabaseRegistry.closeAll();
  }
}
