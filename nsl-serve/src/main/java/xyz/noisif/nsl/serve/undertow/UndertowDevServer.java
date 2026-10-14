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

import xyz.noisif.nsl.common.util.io.IoUtil;
import xyz.noisif.nsl.serve.DevServerProperties;
import xyz.noisif.nsl.serve.GenericDevServer;
import xyz.noisif.nsl.serve.ProxyDefinition;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;

public class UndertowDevServer extends GenericDevServer {
  private Undertow server;

  UndertowDevServer(String[] args) {
    super(args);
  }

  public static void run(String[] args) {
    final GenericDevServer server = new UndertowDevServer(args);
    server.start();
  }

  @Override
  protected void onServerInit(DevServerProperties properties) {
    log.info("Initializing DevServer on port {}...", properties.port());
    final PathHandler pathHandler = Handlers.path();

    for (final ProxyDefinition proxyDef : properties.proxyDefinitions()) {
      final URI rawUri = URI.create(proxyDef.url());
      final String baseOrigin = rawUri.getScheme() + "://" + rawUri.getAuthority();

      final URI targetHostUri =
          proxyDef.stripPrefix()
              ? URI.create(baseOrigin)
              : URI.create(baseOrigin + proxyDef.prefix());

      final LoadBalancingProxyClient proxyClient =
          new LoadBalancingProxyClient().addHost(targetHostUri).setConnectionsPerThread(20);

      final ProxyHandler proxyHandler =
          ProxyHandler.builder().setProxyClient(proxyClient).setMaxRequestTime(3600000).build();
      pathHandler.addPrefixPath(proxyDef.prefix(), proxyHandler);

      log.info(
          "Undertow proxy registered: {} -> {} (StripPrefix: {})",
          proxyDef.prefix(),
          proxyDef.url(),
          proxyDef.stripPrefix());
    }

    final URI staticUri = createStaticDirAndGetUri(properties);
    final File staticDir = new File(staticUri);

    final FileResourceManager resourceManager = new FileResourceManager(staticDir, 1024);
    final ResourceHandler resourceHandler =
        new ResourceHandler(resourceManager)
            .addWelcomeFiles("index.html")
            .setDirectoryListingEnabled(false);

    pathHandler.addPrefixPath("/", new SpaFallbackHandler(staticDir, resourceHandler));

    server =
        Undertow.builder()
            .addHttpListener(properties.port(), properties.host())
            .setHandler(pathHandler)
            .build();
    server.start();

    final int resolvedPort = resolvePort(properties);
    log.info("Undertow DevServer is up and running on http://localhost:{}", resolvedPort);
    onServerReady(resolvedPort);
  }

  @Override
  protected void onServerStopped() {
    IoUtil.closeQuietly(server, Undertow::stop);
  }

  private int resolvePort(DevServerProperties properties) {
    return server.getListenerInfo().stream()
        .filter(i -> i.getProtcol().contains("http"))
        .map(i -> ((InetSocketAddress) i.getAddress()).getPort())
        .findFirst()
        .orElse(properties.port());
  }
}
