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
import xyz.noisif.nsl.http.annotation.HttpController;
import xyz.noisif.nsl.http.annotation.RequestMapping;
import xyz.noisif.nsl.http.resolver.ArgumentResolver;
import xyz.noisif.nsl.http.route.Route;
import xyz.noisif.nsl.http.route.Router;
import xyz.noisif.nsl.net.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

class RouteScanner {
  private static final Logger LOG = LoggerFactory.getLogger(RouteScanner.class);

  private final ComponentProvider componentProvider;
  private final Router router;
  private final Set<ArgumentResolver> resolvers;

  private int registeredRoutesCount = 0;

  RouteScanner(
      ComponentProvider componentProvider, Router router, Set<ArgumentResolver> resolvers) {
    this.componentProvider = componentProvider;
    this.router = router;
    this.resolvers = resolvers;
  }

  void scan() {
    final Collection<Object> instances =
        componentProvider.getInstancesAnnotatedWith(HttpController.class);
    for (final Object instance : instances) {
      registerRoutesForInstance(instance);
    }
    LOG.info("Initialized {} HTTP controller(s)", instances.size());
    LOG.info("Total routes registered: {}", registeredRoutesCount);
  }

  private void registerRoutesForInstance(Object instance) {
    final Class<?> clazz = instance.getClass();
    final String basePath = clazz.getAnnotation(HttpController.class).value();
    for (final Method method : clazz.getDeclaredMethods()) {
      if (method.isAnnotationPresent(RequestMapping.class)) {
        registerMethodRoutes(instance, basePath, method);
        validateMethod(method);
      }
    }
  }

  private void validateMethod(Method method) {
    for (final ArgumentResolver resolver : resolvers) {
      resolver.validate(method);
    }
  }

  private void registerMethodRoutes(Object instance, String basePath, Method method) {
    final RequestMapping mapping = method.getAnnotation(RequestMapping.class);
    final String[] paths = mapping.value();
    final HttpMethod httpMethod = mapping.method();
    for (final String path : paths) {
      final String fullPath = PathUtil.combinePaths(basePath, path);
      router.addRoute(httpMethod.name(), fullPath, new Route(instance, method, path));
      registeredRoutesCount++;
      LOG.debug(
          "Registered route: [{} {}] -> {}.{}()",
          httpMethod,
          fullPath,
          instance.getClass().getSimpleName(),
          method.getName());
    }
  }
}
