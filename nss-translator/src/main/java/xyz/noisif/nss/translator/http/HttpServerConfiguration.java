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
package xyz.noisif.nss.translator.http;

import xyz.noisif.nsl.codec.serialization.SerializerRegistry;
import xyz.noisif.nsl.codec.serialization.json.jackson.JacksonSerializer;
import xyz.noisif.nsl.codec.serialization.raw.RawByteSerializer;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.http.HttpServer;
import xyz.noisif.nsl.http.jetty.JettyHttpServer;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.util.Set;

@Singleton
class HttpServerConfiguration {
  @Produces
  @Singleton
  HttpServer httpServer(ComponentProvider componentProvider) {
    return JettyHttpServer.builder()
        .componentProvider(componentProvider)
        .serializerRegistry(
            SerializerRegistry.createDefault()
                .register(JacksonSerializer.createDefaultStrictMapper())
                .register(RawByteSerializer.createDefault()))
        .ignoredPaths(Set.of())
        .port(9094) /* TODO: incoming from config server */
        .build();
  }
}
