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
package xyz.noisif.nss.ingestor.graal;

import xyz.noisif.nss.ingestor.graal.scripting.GraalJsEngine;
import xyz.noisif.nss.ingestor.graal.scripting.IngestorScript;
import xyz.noisif.nss.ingestor.graal.scripting.JsEngine;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
class JsEngineConfiguration {
  @Produces
  @Singleton
  GraalJsEngine jsEngine() {
    return GraalJsEngine.builder().withLibrary(IngestorScript.YARN_PARSER).build();
  }

  @Produces
  @Singleton
  JsEngine jsEngine(GraalJsEngine jsEngine) {
    return jsEngine;
  }
}
