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
package xyz.noisif.nsl.http.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.util.CryptoUtil;
import xyz.noisif.nsl.common.util.PathUtil;
import xyz.noisif.nsl.common.util.StringUtil;
import xyz.noisif.nsl.common.util.io.IoUtil;
import xyz.noisif.nsl.http.HttpRequest;
import xyz.noisif.nsl.http.HttpResponse;
import xyz.noisif.nsl.http.Interceptor;
import xyz.noisif.nsl.http.acceptor.HttpAcceptorBinding;
import xyz.noisif.nsl.net.http.header.CommonHttpHeaderName;
import xyz.noisif.nsl.net.http.header.CommonHttpHeaderValue;

import java.io.InputStream;
import java.net.URL;

public class StaticResourceInterceptor implements Interceptor<StaticResourceDecision> {
  private static final Logger LOG = LoggerFactory.getLogger(StaticResourceInterceptor.class);

  private final HttpAcceptorBinding binding;
  private volatile String cachedIndexHtml = null; // might be thread-unsafe

  public StaticResourceInterceptor(HttpAcceptorBinding binding) {
    this.binding = binding;
  }

  @Override
  public StaticResourceDecision intercept(HttpRequest request, HttpResponse response)
      throws Exception {
    final String path = request.getPath();
    LOG.trace("Intercepting static resource request for path: {}", path);

    final String baseDir = binding.getStaticContentBase();
    if ("/".equals(path) || "/index.html".equals(path)) {
      return serveIndexHtmlWithNonce(response, baseDir);
    }
    final String cleanResourcePath = resolveResourcePath(baseDir, path);
    if (IoUtil.getResourceUrl(cleanResourcePath) == null) {
      LOG.trace("Resource not found on classpath: {}", cleanResourcePath);
      final boolean isHtml = isHtmlExpected(path, request.getHeader(CommonHttpHeaderName.ACCEPT));
      if (binding.isSpaRoutingEnabled() && isHtml) {
        LOG.debug("SPA routing enabled and HTML expected, attempting fallback for path: {}", path);
        return serveIndexHtmlWithNonce(response, baseDir);
      }
      LOG.debug("No static resource or SPA fallback matched for path: {}, passing to API", path);
      return StaticResourceDecision.PASS_TO_API;
    }
    if (isDirectoryMissingWelcomeFile(path, cleanResourcePath)) {
      LOG.debug("Directory requested without welcome file: {}, passing to API", path);
      return StaticResourceDecision.PASS_TO_API;
    }
    LOG.debug("Delegating static resource handling to default web server for path: {}", path);
    return StaticResourceDecision.CONTINUE_WITH_DEFAULT_HANDLER;
  }

  private StaticResourceDecision serveIndexHtmlWithNonce(HttpResponse response, String baseDir)
      throws Exception {
    String rawHtml = cachedIndexHtml;
    if (rawHtml == null) {
      final String indexPath = PathUtil.removeLeadingSlashes(baseDir + "/index.html");
      final URL indexUrl = IoUtil.getResourceUrl(indexPath);
      if (indexUrl == null) {
        LOG.warn(
            "SPA routing is enabled but fallback file '{}' is missing in resources", indexPath);
        return StaticResourceDecision.PASS_TO_API;
      }
      try (final InputStream inputStream = indexUrl.openStream()) {
        rawHtml = StringUtil.create(inputStream.readAllBytes());
        cachedIndexHtml = rawHtml;
      }
    }
    final String nonce = CryptoUtil.generateNonce(16);
    LOG.trace("Serving SPA file with nonce tag: {}", nonce);
    final String finalHtml = rawHtml.replace("%NONCE%", nonce);
    response.setHeader(CommonHttpHeaderName.CONTENT_TYPE, CommonHttpHeaderValue.TEXT_HTML);
    response.setHeader(
        CommonHttpHeaderName.CONTENT_SECURITY_POLICY,
        "script-src 'self' 'nonce-" + nonce + "'; style-src 'self' 'nonce-" + nonce + "';");

    response.write(finalHtml, true);
    LOG.debug("Successfully served SPA fallback file for requested path");
    return StaticResourceDecision.HANDLED_BY_FALLBACK;
  }

  private String resolveResourcePath(String baseDir, String path) {
    final String relativePath = PathUtil.removeLeadingSlashes(path);
    return PathUtil.removeLeadingSlashes(baseDir + "/" + relativePath);
  }

  private boolean isHtmlExpected(String path, String acceptHeader) {
    if (acceptHeader != null && acceptHeader.contains("text/html")) {
      return true;
    }
    return path != null && path.endsWith(".html");
  }

  private boolean isDirectoryMissingWelcomeFile(String path, String cleanResourcePath) {
    if (path == null || !path.endsWith("/")) {
      return false;
    }
    final String indexPath = PathUtil.removeLeadingSlashes(cleanResourcePath + "index.html");
    return IoUtil.getResourceUrl(indexPath) == null;
  }
}
