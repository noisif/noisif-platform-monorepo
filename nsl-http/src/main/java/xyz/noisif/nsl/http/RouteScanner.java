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
package xyz.noisif.nsl.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.util.PathUtil;
import xyz.noisif.nsl.http.acceptor.ConnectionAcceptor;
import xyz.noisif.nsl.http.annotation.RequestMapping;
import xyz.noisif.nsl.http.resolver.ArgumentResolver;
import xyz.noisif.nsl.http.route.Route;
import xyz.noisif.nsl.http.route.Router;
import xyz.noisif.nsl.net.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

class RouteScanner {
  private static final Logger LOG = LoggerFactory.getLogger(RouteScanner.class);

  private final ComponentProvider componentProvider;
  private final Router router;
  private final Set<ArgumentResolver> resolvers;
  private final Set<ConnectionAcceptor> connectionAcceptors;

  private int registeredRoutesCount = 0;

  RouteScanner(
      ComponentProvider componentProvider,
      Router router,
      Set<ArgumentResolver> resolvers,
      Set<ConnectionAcceptor> connectionAcceptors) {
    this.componentProvider = componentProvider;
    this.router = router;
    this.resolvers = resolvers;
    this.connectionAcceptors = connectionAcceptors;
  }

  void scan() {
    final Collection<HttpController> instances =
        componentProvider.getInstancesOf(HttpController.class);
    for (final HttpController instance : instances) {
      if (!Collections.disjoint(connectionAcceptors, instance.getConnectionAcceptors())) {
        registerRoutesForInstance(instance);
      }
    }
    LOG.info(
        "Initialized {} HTTP controller(s) for acceptors {}",
        instances.size(),
        connectionAcceptors);
    LOG.info("Total routes registered in this router: {}", registeredRoutesCount);
  }

  int getRegisteredRoutesCount() {
    return registeredRoutesCount;
  }

  private void registerRoutesForInstance(HttpController instance) {
    final Class<?> clazz = instance.getClass();
    final String basePath = instance.getBasePath();
    for (final Method method : clazz.getDeclaredMethods()) {
      if (method.isAnnotationPresent(RequestMapping.class)) {
        registerMethodRoutes(instance, basePath, method);
        validateMethod(instance, method);
      }
    }
  }

  private void validateMethod(HttpController instance, Method method) {
    for (final ArgumentResolver resolver : resolvers) {
      resolver.validate(instance, method);
    }
  }

  private void registerMethodRoutes(HttpController instance, String basePath, Method method) {
    final RequestMapping mapping = method.getAnnotation(RequestMapping.class);
    final String[] paths = mapping.value();
    final HttpMethod httpMethod = mapping.method();
    for (final String path : paths) {
      final String fullPath = PathUtil.combinePaths(basePath, path);
      router.addRoute(httpMethod.name(), fullPath, new Route(instance, method, path));
      registeredRoutesCount++;
      LOG.debug(
          "Registered route for {}: [{} {}] -> {}.{}()",
          connectionAcceptors,
          httpMethod,
          fullPath,
          instance.getClass().getSimpleName(),
          method.getName());
    }
  }
}
