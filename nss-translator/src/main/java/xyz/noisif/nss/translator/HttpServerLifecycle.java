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
package xyz.noisif.nss.translator;

import xyz.noisif.nsl.codec.serialization.SerializerRegistry;
import xyz.noisif.nsl.codec.serialization.json.JacksonSerializer;
import xyz.noisif.nsl.codec.serialization.raw.RawByteSerializer;
import xyz.noisif.nsl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.reflect.ClassScanner;
import xyz.noisif.nsl.http.HttpServer;
import xyz.noisif.nsl.http.jetty.JettyHttpServer;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Set;

@Singleton
class HttpServerLifecycle implements LifecycleHook {
  private final HttpServer httpServer;

  @Inject
  HttpServerLifecycle(ComponentProvider componentProvider) {
    httpServer =
        JettyHttpServer.builder()
            .componentProvider(componentProvider)
            .serializerRegistry(
                SerializerRegistry.createDefault()
                    .register(JacksonSerializer.createDefaultStrictMapper())
                    .register(RawByteSerializer.createDefault()))
            .ignoredPaths(Set.of())
            .port(9094) /* TODO: incoming from config server */
            .build();
  }

  @Override
  public void onStart(ComponentProvider componentProvider, ClassScanner scanner) {
    httpServer.start();
  }

  @Override
  public void onStop() {
    httpServer.close();
  }
}
