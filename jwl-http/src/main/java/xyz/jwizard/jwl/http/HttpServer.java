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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import xyz.jwizard.jwl.codec.serialization.MessageSerializer;
import xyz.jwizard.jwl.codec.serialization.SerializerRegistry;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;
import xyz.jwizard.jwl.codec.serialization.json.JsonSerializer;
import xyz.jwizard.jwl.common.bootstrap.lifecycle.IdempotentService;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.common.util.CollectionUtil;
import xyz.jwizard.jwl.http.annotation.Validator;
import xyz.jwizard.jwl.http.exception.handler.AnnotatedExceptionHandler;
import xyz.jwizard.jwl.http.exception.handler.BadRequestExceptionHandler;
import xyz.jwizard.jwl.http.exception.handler.ExceptionHandler;
import xyz.jwizard.jwl.http.exception.handler.GlobalExceptionHandler;
import xyz.jwizard.jwl.http.exception.handler.RequestTooLargeExceptionHandler;
import xyz.jwizard.jwl.http.filter.HttpFilter;
import xyz.jwizard.jwl.http.resolver.ArgumentResolver;
import xyz.jwizard.jwl.http.resolver.HttpRequestArgumentResolver;
import xyz.jwizard.jwl.http.resolver.PathVariableResolver;
import xyz.jwizard.jwl.http.resolver.RequestParamResolver;
import xyz.jwizard.jwl.http.resolver.body.RequestBodyResolver;
import xyz.jwizard.jwl.http.route.Router;
import xyz.jwizard.jwl.http.route.TrieRouter;
import xyz.jwizard.jwl.http.validation.AnnotationValidator;
import xyz.jwizard.jwl.http.validation.ValidationHandler;
import xyz.jwizard.jwl.http.writer.JsonResponseWriter;
import xyz.jwizard.jwl.http.writer.ResponseEntityResponseWriter;
import xyz.jwizard.jwl.http.writer.ResponseWriter;
import xyz.jwizard.jwl.http.writer.StringResponseWriter;
import xyz.jwizard.jwl.http.writer.VoidResponseWriter;

public abstract class HttpServer extends IdempotentService {
    protected final ComponentProvider componentProvider;
    protected final Router router;
    protected final Set<String> ignoredPaths;
    protected final int port;

    protected final List<HttpFilter> filters; // must be list or sorted set!
    protected final Set<ArgumentResolver> resolvers;
    protected final Set<ResponseWriter> writers;
    protected final Set<ExceptionHandler> exceptionHandlers;

    private final Set<String> defaultIgnoredPaths = Set.of(
        "/favicon.ico",
        "/robots.txt"
    );

    protected HttpServer(AbstractBuilder<?> builder) {
        componentProvider = builder.componentProvider;
        router = new TrieRouter();
        port = builder.port;
        ignoredPaths = combinePaths(builder.ignoredPaths);
        filters = componentProvider.getInstancesOf(HttpFilter.class)
            .stream()
            .sorted(HttpFilter.COMPARATOR)
            .toList();
        final Set<AnnotationValidator<?>> validators = componentProvider
            .getInstancesAnnotatedWith(Validator.class)
            .stream()
            .map(obj -> (AnnotationValidator<?>) obj)
            .collect(Collectors.toSet());
        resolvers = CollectionUtil.linkedSetOf(
            new PathVariableResolver(router),
            new RequestParamResolver(),
            new HttpRequestArgumentResolver(),
            new RequestBodyResolver(builder.serializerRegistry, new ValidationHandler(validators))
        );
        writers = initWriters(builder.serializerRegistry);
        exceptionHandlers = CollectionUtil.linkedSetOf(
            new AnnotatedExceptionHandler(),
            new BadRequestExceptionHandler(),
            new RequestTooLargeExceptionHandler(),
            new GlobalExceptionHandler()
        );
        log.info("HTTP server initialized with:");
        log.info("-- {} filter(s) (via reflect api)", filters.size());
        log.info("-- {} validator(s) (via reflect api)", validators.size());
        log.info("-- {} resolver(s) (statically typed)", resolvers.size());
        log.info("-- {} writer(s) (statically typed)", writers.size());
        log.info("-- {} exception handler(s) (statically typed)", exceptionHandlers.size());
    }

    protected HttpRequestHandler prepareRequestHandler() {
        log.info("Scanning routes and preparing HTTP request handler");
        final RouteScanner scanner = new RouteScanner(componentProvider, router, resolvers);
        scanner.scan();
        return new HttpRequestHandler(
            router,
            ignoredPaths,
            filters,
            resolvers,
            writers,
            exceptionHandlers
        );
    }

    private Set<String> combinePaths(Set<String> customPaths) {
        final Set<String> combined = new HashSet<>(defaultIgnoredPaths);
        combined.addAll(customPaths);
        return Set.copyOf(combined);
    }

    private Set<ResponseWriter> initWriters(SerializerRegistry<MessageSerializer> registry) {
        final Set<ResponseWriter> delegates = CollectionUtil.linkedSetOf(
            new VoidResponseWriter(),
            new StringResponseWriter()
        );
        final JsonSerializer jsonSerializer = (JsonSerializer) registry.get(StandardSerializerFormat.JSON);
        if (jsonSerializer != null) {
            delegates.add(new JsonResponseWriter(jsonSerializer));
        }
        final Set<ResponseWriter> all = CollectionUtil.linkedSetOf(
            new ResponseEntityResponseWriter(delegates)
        );
        all.addAll(delegates);
        return all;
    }

    public abstract int getLocalPort();

    protected abstract static class AbstractBuilder<B extends AbstractBuilder<B>> {
        private final Set<String> ignoredPaths = new HashSet<>();

        private ComponentProvider componentProvider;
        private SerializerRegistry<MessageSerializer> serializerRegistry;
        private int port = 8080;

        protected AbstractBuilder() {
        }

        protected abstract B self();

        public B componentProvider(ComponentProvider componentProvider) {
            this.componentProvider = componentProvider;
            return self();
        }

        public B serializerRegistry(SerializerRegistry<MessageSerializer> serializerRegistry) {
            this.serializerRegistry = serializerRegistry;
            return self();
        }

        public B port(int port) {
            this.port = port;
            return self();
        }

        public B ignoredPaths(Set<String> paths) {
            this.ignoredPaths.addAll(paths);
            return self();
        }

        protected void validate() {
            Assert.notNull(componentProvider, "ComponentProvider cannot be null");
            Assert.notNull(serializerRegistry, "SerializerRegistry cannot be null");
            Assert.state(port >= 0 && port < 65536, "Invalid port number");
        }

        public abstract HttpServer build();
    }
}
