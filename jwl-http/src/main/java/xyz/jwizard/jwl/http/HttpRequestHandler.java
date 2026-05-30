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
package xyz.jwizard.jwl.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.cache.MultiProviderCache;
import xyz.jwizard.jwl.common.cache.ProviderCache;
import xyz.jwizard.jwl.http.exception.handler.ExceptionHandler;
import xyz.jwizard.jwl.http.filter.HttpFilter;
import xyz.jwizard.jwl.http.resolver.ArgumentResolver;
import xyz.jwizard.jwl.http.route.MatchResult;
import xyz.jwizard.jwl.http.route.Route;
import xyz.jwizard.jwl.http.route.Router;
import xyz.jwizard.jwl.http.writer.ResponseWriter;
import xyz.jwizard.jwl.net.http.HttpStatus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Set;

public class HttpRequestHandler {
  private static final Logger LOG = LoggerFactory.getLogger(HttpRequestHandler.class);

  private final Router router;
  private final Set<String> ignoredPaths; // must be set! (O(1), table/list are much slower)

  // cache for O(1) complexity, lists are BAD :(
  private final ProviderCache<Parameter, Parameter, ArgumentResolver> resolverCache;
  private final ProviderCache<Class<?>, Object, ResponseWriter> writerCache;
  private final ProviderCache<Class<? extends Throwable>, Throwable, ExceptionHandler>
      exceptionCache;
  private final MultiProviderCache<Route, Route, HttpFilter> filterCache;

  public HttpRequestHandler(
      Router router,
      Set<String> ignoredPaths,
      List<HttpFilter> sortedFilters,
      Set<ArgumentResolver> resolvers,
      Set<ResponseWriter> writers,
      Set<ExceptionHandler> exceptionHandlers) {
    this.router = router;
    this.ignoredPaths = ignoredPaths;

    resolverCache = new ProviderCache<>(resolvers, ArgumentResolver::supports);
    writerCache = new ProviderCache<>(writers, ResponseWriter::supports);
    exceptionCache = new ProviderCache<>(exceptionHandlers, ExceptionHandler::supports);
    filterCache = new MultiProviderCache<>(sortedFilters, HttpFilter::supports);
  }

  public void processRequest(HttpRequest req, HttpResponse res) throws Exception {
    final String method = req.getMethod();
    final String path = req.getPath();

    LOG.debug("Incoming request: {} {}", method, path);

    if (ignoredPaths.contains(path)) {
      LOG.debug("Path {} is in ignored paths list, skipping", path);
      finish(res, HttpStatus.NO_CONTENT_204);
      return;
    }
    final MatchResult match = router.findRoute(method, path);
    if (match == null) {
      LOG.debug("No route found for {} {}", method, path);
      finish(res, HttpStatus.NOT_FOUND_404);
      return;
    }
    final Route route = match.route();
    LOG.debug(
        "Route matched: {} -> {}.{}()",
        path,
        route.instance().getClass().getSimpleName(),
        route.method().getName());

    final List<HttpFilter> activeFilters = filterCache.get(route, route);
    LOG.debug(
        "Active filters for {}: {}",
        route.path(),
        activeFilters.stream().map(f -> f.getClass().getSimpleName()).toList());

    for (final HttpFilter filter : activeFilters) {
      if (!filter.preHandle(req, res)) {
        LOG.debug("Request stopped by filter: {}", filter.getClass().getSimpleName());
        res.end();
        return;
      }
    }
    final Object result = execute(req, match);
    processResponse(res, result);
  }

  public void handleException(HttpRequest req, HttpResponse res, Exception ex) {
    final Throwable cause = (ex instanceof InvocationTargetException) ? ex.getCause() : ex;
    final Class<? extends Throwable> causeClass = cause.getClass();
    final ExceptionHandler handler = exceptionCache.get(causeClass, cause);
    if (handler != null) {
      handler.handle(req, res, cause);
      return;
    }
    finish(res, HttpStatus.INTERNAL_SERVER_ERROR_500);
  }

  private Object execute(HttpRequest req, MatchResult match) throws Exception {
    final Method actionMethod = match.route().method();
    actionMethod.setAccessible(true);

    final Parameter[] parameters = actionMethod.getParameters();
    final Object[] args = new Object[parameters.length];
    if (parameters.length > 0) {
      LOG.debug("Resolving {} parameters for method {}", parameters.length, actionMethod.getName());
    }
    for (int i = 0; i < parameters.length; i++) {
      final Parameter param = parameters[i];
      final ArgumentResolver resolver = resolverCache.get(param, param);
      if (resolver != null) {
        LOG.debug(
            "Parameter '{}' [{}] resolved by {}",
            param.getName(),
            param.getType().getSimpleName(),
            resolver.getClass().getSimpleName());
        args[i] = resolver.resolve(param, req, match);
      } else {
        LOG.warn(
            "No resolver found for parameter '{}' [{}]",
            param.getName(),
            param.getType().getSimpleName());
      }
    }
    return actionMethod.invoke(match.route().instance(), args);
  }

  private void processResponse(HttpResponse res, Object result) throws Exception {
    if (!(result instanceof ResponseEntity<?>)) {
      res.setStatus(HttpStatus.OK_200);
    }
    final Class<?> resultClass = (result == null) ? void.class : result.getClass();
    final ResponseWriter writer = writerCache.get(resultClass, result);
    if (writer != null) {
      writer.write(res, result);
      return;
    }
    LOG.error("No suitable ResponseWriter found for result class: {}", resultClass);
    finish(res, HttpStatus.INTERNAL_SERVER_ERROR_500);
  }

  private void finish(HttpResponse response, HttpStatus status) {
    response.setStatus(status);
    response.end();
  }
}
