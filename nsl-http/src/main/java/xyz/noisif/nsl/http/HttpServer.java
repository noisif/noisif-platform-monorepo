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

import xyz.noisif.nsl.codec.serialization.MessageSerializer;
import xyz.noisif.nsl.codec.serialization.SerializerRegistry;
import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;
import xyz.noisif.nsl.codec.serialization.json.JsonSerializer;
import xyz.noisif.nsl.common.bootstrap.lifecycle.IdempotentService;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.common.util.CollectionUtil;
import xyz.noisif.nsl.http.acceptor.ConnectionAcceptor;
import xyz.noisif.nsl.http.acceptor.HttpAcceptorBinding;
import xyz.noisif.nsl.http.annotation.Validator;
import xyz.noisif.nsl.http.exception.handler.AnnotatedExceptionHandler;
import xyz.noisif.nsl.http.exception.handler.BadRequestExceptionHandler;
import xyz.noisif.nsl.http.exception.handler.ExceptionHandler;
import xyz.noisif.nsl.http.exception.handler.GlobalExceptionHandler;
import xyz.noisif.nsl.http.exception.handler.RequestTooLargeExceptionHandler;
import xyz.noisif.nsl.http.filter.HttpFilter;
import xyz.noisif.nsl.http.resolver.ArgumentResolver;
import xyz.noisif.nsl.http.resolver.HttpRequestArgumentResolver;
import xyz.noisif.nsl.http.resolver.PathVariableResolver;
import xyz.noisif.nsl.http.resolver.RequestParamResolver;
import xyz.noisif.nsl.http.resolver.body.RequestBodyResolver;
import xyz.noisif.nsl.http.validation.AnnotationValidator;
import xyz.noisif.nsl.http.validation.ValidationHandler;
import xyz.noisif.nsl.http.writer.JsonResponseWriter;
import xyz.noisif.nsl.http.writer.ResponseEntityResponseWriter;
import xyz.noisif.nsl.http.writer.ResponseWriter;
import xyz.noisif.nsl.http.writer.StringResponseWriter;
import xyz.noisif.nsl.http.writer.VoidResponseWriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class HttpServer extends IdempotentService {
  protected final ComponentProvider componentProvider;
  protected final Map<ConnectionAcceptor, HttpAcceptorBinding> bindings;

  protected final List<HttpFilter> filters; // must be list or sorted set!
  protected final Set<ArgumentResolver> resolvers;
  protected final Set<ResponseWriter> writers;
  protected final Set<ExceptionHandler> exceptionHandlers;

  protected HttpServer(AbstractBuilder<?> builder) {
    componentProvider = builder.componentProvider;
    bindings = builder.bindings;
    filters =
        componentProvider.getInstancesOf(HttpFilter.class).stream()
            .sorted(HttpFilter.COMPARATOR)
            .toList();
    final Set<AnnotationValidator<?>> validators =
        componentProvider.getInstancesAnnotatedWith(Validator.class).stream()
            .map(obj -> (AnnotationValidator<?>) obj)
            .collect(Collectors.toSet());
    resolvers =
        CollectionUtil.linkedSetOf(
            new RequestParamResolver(),
            new HttpRequestArgumentResolver(),
            new RequestBodyResolver(builder.serializerRegistry, new ValidationHandler(validators)));
    writers = initWriters(builder.serializerRegistry);
    exceptionHandlers =
        CollectionUtil.linkedSetOf(
            new AnnotatedExceptionHandler(),
            new BadRequestExceptionHandler(),
            new RequestTooLargeExceptionHandler(),
            new GlobalExceptionHandler());
    log.info("HTTP server initialized with:");
    log.info("-- {} binding(s) configured", bindings.size());
    log.info("-- {} filter(s) (via reflect api)", filters.size());
    log.info("-- {} validator(s) (via reflect api)", validators.size());
    log.info("-- {} resolver(s) (statically typed)", resolvers.size());
    log.info("-- {} writer(s) (statically typed)", writers.size());
    log.info("-- {} exception handler(s) (statically typed)", exceptionHandlers.size());
  }

  protected Map<ConnectionAcceptor, HttpRequestHandler> prepareRequestHandler() {
    log.info("Scanning routes and preparing HTTP request handler");
    final Map<ConnectionAcceptor, HttpRequestHandler> handlers = new HashMap<>();
    for (final HttpAcceptorBinding binding : bindings.values()) {
      final ConnectionAcceptor acceptor = binding.getConnectionAcceptor();

      final List<HttpFilter> localFilters =
          filters.stream()
              .filter(
                  filter ->
                      !Collections.disjoint(Set.of(acceptor), filter.getConnectionAcceptors()))
              .toList();

      final Set<ArgumentResolver> localResolvers = new HashSet<>(resolvers);
      localResolvers.add(new PathVariableResolver(binding.getRouter()));
      final RouteScanner classScanner =
          new RouteScanner(
              componentProvider, binding.getRouter(), localResolvers, Set.of(acceptor));
      classScanner.scan();

      final HttpRequestHandler handler =
          new HttpRequestHandler(binding, localFilters, localResolvers, writers, exceptionHandlers);
      handlers.put(acceptor, handler);

      log.info(
          "Successfully prepared HttpRequestHandler for acceptor: {} on port: {} "
              + "(routes: {}, filters: {})",
          acceptor.getConnectionAcceptorName(),
          binding.getPort(),
          classScanner.getRegisteredRoutesCount(),
          localFilters.size());
    }
    return handlers;
  }

  private Set<ResponseWriter> initWriters(SerializerRegistry<MessageSerializer> registry) {
    final Set<ResponseWriter> delegates =
        CollectionUtil.linkedSetOf(new VoidResponseWriter(), new StringResponseWriter());
    final JsonSerializer jsonSerializer =
        (JsonSerializer) registry.get(StandardSerializerFormat.JSON);
    if (jsonSerializer != null) {
      delegates.add(new JsonResponseWriter(jsonSerializer));
    }
    final Set<ResponseWriter> all =
        CollectionUtil.linkedSetOf(new ResponseEntityResponseWriter(delegates));
    all.addAll(delegates);
    return all;
  }

  public abstract int getLocalPort(ConnectionAcceptor acceptor);

  public int getLocalPort() {
    return getLocalPort(ConnectionAcceptor.DEFAULT);
  }

  protected abstract static class AbstractBuilder<B extends AbstractBuilder<B>> {
    private final Map<ConnectionAcceptor, HttpAcceptorBinding> bindings = new HashMap<>();

    private ComponentProvider componentProvider;
    private SerializerRegistry<MessageSerializer> serializerRegistry;

    protected AbstractBuilder() {}

    protected abstract B self();

    public B componentProvider(ComponentProvider componentProvider) {
      this.componentProvider = componentProvider;
      return self();
    }

    public B serializerRegistry(SerializerRegistry<MessageSerializer> serializerRegistry) {
      this.serializerRegistry = serializerRegistry;
      return self();
    }

    public B withBinding(HttpAcceptorBinding binding) {
      this.bindings.put(binding.getConnectionAcceptor(), binding);
      return self();
    }

    protected void validate() {
      Assert.notNull(componentProvider, "componentProvider");
      Assert.notNull(serializerRegistry, "serializerRegistry");
      Assert.notEmpty(bindings.entrySet(), "bindings");
      Assert.notNullAll(bindings, "bindings");
    }

    public abstract HttpServer build();
  }
}
