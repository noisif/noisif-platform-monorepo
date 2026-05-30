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
package xyz.jwizard.jws.api;

import xyz.jwizard.jwl.codec.serialization.SerializerRegistry;
import xyz.jwizard.jwl.codec.serialization.json.JacksonSerializer;
import xyz.jwizard.jwl.codec.serialization.raw.RawByteSerializer;
import xyz.jwizard.jwl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.http.HttpServer;
import xyz.jwizard.jwl.http.jetty.JettyHttpServer;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
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
            .port(9091) /* TODO: incoming from config server */
            .build();
  }

  @Override
  public List<Class<? extends LifecycleHook>> dependsOn() {
    return List.of(KvServerLifecycle.class, SqlClientLifecycle.class);
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
