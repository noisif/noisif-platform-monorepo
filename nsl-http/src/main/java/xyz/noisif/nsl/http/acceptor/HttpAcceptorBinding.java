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
package xyz.noisif.nsl.http.acceptor;

import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.http.Interceptor;
import xyz.noisif.nsl.http.resource.StaticResourceDecision;
import xyz.noisif.nsl.http.resource.StaticResourceInterceptor;
import xyz.noisif.nsl.http.route.Router;
import xyz.noisif.nsl.http.route.TrieRouter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class HttpAcceptorBinding {
  private static final Set<String> DEFAULT_IGNORED_PATHS = Set.of("/favicon.ico", "/robots.txt");

  private final ConnectionAcceptor connectionAcceptor;
  private final int port;
  private final boolean staticContentEnabled;
  private final String staticContentBase;
  private final Interceptor<StaticResourceDecision> staticResourcesInterceptor;
  private final boolean spaRoutingEnabled;
  private final Set<String> ignoredPaths;
  private final Router router;

  private HttpAcceptorBinding(Builder builder) {
    connectionAcceptor = builder.connectionAcceptor;
    port = builder.port;
    staticContentEnabled = builder.staticContentEnabled;
    staticContentBase = builder.staticContentBase;
    spaRoutingEnabled = builder.spaRoutingEnabled;
    staticResourcesInterceptor = builder.interceptorFactory.apply(this);
    ignoredPaths = builder.ignoredPaths;
    router = builder.router;
  }

  public static Builder builder() {
    return new Builder();
  }

  public ConnectionAcceptor getConnectionAcceptor() {
    return connectionAcceptor;
  }

  public int getPort() {
    return port;
  }

  public boolean isStaticContentEnabled() {
    return staticContentEnabled;
  }

  public String getStaticContentBase() {
    return staticContentBase;
  }

  public boolean isSpaRoutingEnabled() {
    return spaRoutingEnabled;
  }

  public Interceptor<StaticResourceDecision> getStaticResourcesInterceptor() {
    return staticResourcesInterceptor;
  }

  public Set<String> getIgnoredPaths() {
    return ignoredPaths;
  }

  public Router getRouter() {
    return router;
  }

  public static class Builder {
    private ConnectionAcceptor connectionAcceptor = ConnectionAcceptor.DEFAULT;
    private int port;
    private boolean staticContentEnabled = false;
    private String staticContentBase = "webroot";
    private boolean spaRoutingEnabled = false;
    private Function<HttpAcceptorBinding, Interceptor<StaticResourceDecision>> interceptorFactory =
        StaticResourceInterceptor::new;
    private final Set<String> ignoredPaths = new HashSet<>(DEFAULT_IGNORED_PATHS);
    private Router router = new TrieRouter();

    public Builder connectionAcceptor(ConnectionAcceptor connectionAcceptor) {
      this.connectionAcceptor = connectionAcceptor;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder staticContent(boolean enabled) {
      staticContentEnabled = enabled;
      if (enabled) {
        ignoredPaths.removeAll(DEFAULT_IGNORED_PATHS);
      }
      return this;
    }

    public Builder staticContent(boolean enabled, String baseFolder) {
      staticContentBase = baseFolder;
      return staticContent(enabled);
    }

    public Builder setStaticResourceInterceptor(
        Function<HttpAcceptorBinding, Interceptor<StaticResourceDecision>> interceptorFactory) {
      this.interceptorFactory = interceptorFactory;
      return this;
    }

    public Builder spaRouting(boolean enabled) {
      spaRoutingEnabled = enabled;
      return this;
    }

    public Builder ignoredPaths(Set<String> paths) {
      ignoredPaths.addAll(paths);
      return this;
    }

    public Builder router(Router router) {
      this.router = router;
      return this;
    }

    public HttpAcceptorBinding build() {
      Assert.notNull(connectionAcceptor, "connectionAcceptor");
      Assert.minMaxRange(port, 0, 65536, "port");
      Assert.notNull(staticContentBase, "staticContentBase");
      Assert.notNull(interceptorFactory, "interceptorFactory");
      Assert.notNullAll(ignoredPaths, "ignoredPaths");
      Assert.notNull(router, "router");
      return new HttpAcceptorBinding(this);
    }
  }
}
