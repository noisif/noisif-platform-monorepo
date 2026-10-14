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
package xyz.noisif.nsl.serve.undertow;

import xyz.noisif.nsl.common.util.CryptoUtil;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

import java.io.File;
import java.nio.file.Files;

public class SpaFallbackHandler implements HttpHandler {
  private static final HttpString CSP_HEADER = HttpString.tryFromString("Content-Security-Policy");

  private final File staticDir;
  private final ResourceHandler resourceHandler;

  private volatile String cachedIndexHtml = null;
  private volatile long indexLastModified = 0;

  public SpaFallbackHandler(File staticDir, ResourceHandler resourceHandler) {
    this.staticDir = staticDir;
    this.resourceHandler = resourceHandler;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    if (Methods.GET_STRING.equalsIgnoreCase(exchange.getRequestMethod().toString())) {
      final String path = exchange.getRelativePath();

      final boolean isIndexRequest = "/".equals(path) || "/index.html".equals(path);
      boolean requiresSpaFallback = false;

      if (!isIndexRequest && path != null && !path.contains(".")) {
        final File targetFile = new File(staticDir, path);
        if (!targetFile.exists() || targetFile.isDirectory()) {
          requiresSpaFallback = true;
        }
      }
      if (isIndexRequest || requiresSpaFallback) {
        serveIndexHtmlWithNonce(exchange);
        return;
      }
    }
    resourceHandler.handleRequest(exchange);
  }

  private void serveIndexHtmlWithNonce(HttpServerExchange exchange) throws Exception {
    final File indexFile = new File(staticDir, "index.html");
    if (!indexFile.exists()) {
      resourceHandler.handleRequest(exchange);
      return;
    }
    final long currentLastModified = indexFile.lastModified();
    if (cachedIndexHtml == null || currentLastModified > indexLastModified) {
      cachedIndexHtml = Files.readString(indexFile.toPath());
      indexLastModified = currentLastModified;
    }
    final String nonce = CryptoUtil.generateNonce(16);
    final String finalHtml = cachedIndexHtml.replace("%NONCE%", nonce);

    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=UTF-8");
    exchange
        .getResponseHeaders()
        .put(
            CSP_HEADER,
            "script-src 'self' 'nonce-" + nonce + "'; style-src 'self' 'nonce-" + nonce + "';");
    exchange.getResponseSender().send(finalHtml);
  }
}
