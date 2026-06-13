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
package xyz.noisif.nss.ingestor;

import xyz.noisif.nsl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.reflect.ClassScanner;
import xyz.noisif.nss.ingestor.config.scripting.IngestorScript;
import xyz.noisif.nss.ingestor.scripting.JsEngine;
import xyz.noisif.nss.ingestor.scripting.graal.GraalJsEngine;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
class JsEngineLifecycle implements LifecycleHook {
  private final GraalJsEngine jsEngine;

  JsEngineLifecycle() {
    jsEngine = GraalJsEngine.builder().withLibrary(IngestorScript.YARN_PARSER).build();
  }

  @Override
  public void onStart(ComponentProvider componentProvider, ClassScanner scanner) {
    jsEngine.start();
  }

  @Override
  public void onStop() {
    jsEngine.close();
  }

  @Produces
  @Singleton
  JsEngine jsEngine() {
    return jsEngine;
  }
}
