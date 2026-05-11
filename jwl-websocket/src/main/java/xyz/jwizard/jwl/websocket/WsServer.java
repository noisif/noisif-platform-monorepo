/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jwizard.jwl.websocket;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import xyz.jwizard.jwl.codec.envelope.EnvelopeDataType;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializerFormat;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializerRegistry;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;
import xyz.jwizard.jwl.common.bootstrap.lifecycle.IdempotentService;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.limit.NoOpRateLimiter;
import xyz.jwizard.jwl.common.limit.RateLimiter;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.common.util.CastUtil;
import xyz.jwizard.jwl.websocket.auth.CompositeWsAuthenticator;
import xyz.jwizard.jwl.websocket.auth.WsAuthenticator;
import xyz.jwizard.jwl.websocket.auth.handler.WsAuthFailureHandler;
import xyz.jwizard.jwl.websocket.broadcast.WsBroadcaster;
import xyz.jwizard.jwl.websocket.broadcast.WsMessageSink;
import xyz.jwizard.jwl.websocket.broadcast.WsMessageSinkBroadcaster;
import xyz.jwizard.jwl.websocket.dispatcher.LocalSessionDispatcher;
import xyz.jwizard.jwl.websocket.dispatcher.factory.LocalSessionDispatcherFactory;
import xyz.jwizard.jwl.websocket.listener.CompositeWsMessageListeners;
import xyz.jwizard.jwl.websocket.listener.WsMessageListener;
import xyz.jwizard.jwl.websocket.listener.action.pool.WsActionPool;
import xyz.jwizard.jwl.websocket.listener.lifecycle.CompositeWsLifecycleListener;
import xyz.jwizard.jwl.websocket.listener.lifecycle.WsLifecycleListener;
import xyz.jwizard.jwl.websocket.negotation.WsSerializerResolver;
import xyz.jwizard.jwl.websocket.negotation.WsSerializerResolverFactory;
import xyz.jwizard.jwl.websocket.registry.WsSessionRegistry;
import xyz.jwizard.jwl.websocket.registry.WsSubscriptionRegistry;

public abstract class WsServer extends IdempotentService {
    protected final int port;
    protected final String path;
    protected final long maxMessageSize;
    protected final Duration idleTimeout;

    protected final RateLimiter rateLimiter;
    protected final EnvelopeSerializerRegistry serializerRegistry;
    protected final WsSerializerResolver serializerResolver;
    protected final WsSessionRegistry sessionRegistry;
    protected final WsAuthenticator authenticator;
    protected final WsAuthFailureHandler authFailureHandler;
    protected final WsLifecycleListener lifecycleListener;
    protected final WsMessageListener messageListener;
    protected final LocalSessionDispatcher localSessionDispatcher;
    protected final WsBroadcaster broadcaster;

    protected WsServer(AbstractBuilder<?> builder) {
        port = builder.port;
        path = builder.path;
        maxMessageSize = builder.maxMessageSize;
        idleTimeout = builder.idleTimeout;
        rateLimiter = builder.rateLimiter;
        serializerRegistry = builder.serializerRegistry;
        serializerResolver = builder.serializerResolverFactory.create(serializerRegistry);
        sessionRegistry = builder.sessionRegistry;
        authFailureHandler = builder.authFailureHandler;
        messageListener = loadWsMessageListeners(builder.messageListeners);
        authenticator = loadWsAuthenticators(builder.componentProvider, builder.authenticators);
        lifecycleListener = loadLifecycleListeners(builder.componentProvider);
        localSessionDispatcher = builder.localSessionDispatcherFactory.create(sessionRegistry);
        broadcaster = determinateWsBroadcaster(builder);
    }

    public WsSubscriptionRegistry getWsSubscriptionRegistry() {
        return sessionRegistry;
    }

    public WsBroadcaster getBroadcaster() {
        return broadcaster;
    }

    public LocalSessionDispatcher getLocalSessionDispatcher() {
        return localSessionDispatcher;
    }

    private WsAuthenticator loadWsAuthenticators(ComponentProvider componentProvider,
                                                 List<WsAuthenticator> authenticators) {
        final Collection<WsAuthenticator> reflectAuthenticators = componentProvider
            .getInstancesOf(WsAuthenticator.class);
        reflectAuthenticators.addAll(authenticators);
        final List<WsAuthenticator> sortedAuthenticators = new ArrayList<>(reflectAuthenticators)
            .stream()
            .sorted(WsAuthenticator.COMPARATOR)
            .toList();
        if (log.isDebugEnabled()) {
            final String pipeline = sortedAuthenticators.stream()
                .map(listener -> listener.getClass().getSimpleName())
                .collect(Collectors.joining(" -> "));
            log.debug("CompositeWsAuthenticator initialized with pipeline: {}", pipeline);
        }
        log.info("Load {} ({} via reflection) WS authenticator(s)", sortedAuthenticators.size(),
            reflectAuthenticators.size());
        return new CompositeWsAuthenticator(sortedAuthenticators);
    }

    private WsMessageListener loadWsMessageListeners(List<WsMessageListener> messageListeners) {
        if (log.isDebugEnabled()) {
            final String pipeline = messageListeners.stream()
                .map(listener -> {
                    final WsActionPool pool = listener.getPool();
                    final String name = listener.getClass().getSimpleName();
                    return pool != null ? name + "[" + pool + "]" : name;
                })
                .collect(Collectors.joining(" -> "));
            log.debug("CompositeWsMessageListeners initialized with pipeline: {}", pipeline);
        }
        log.info("Load {} WS message listener(s)", messageListeners.size());
        return new CompositeWsMessageListeners(messageListeners);
    }

    private WsLifecycleListener loadLifecycleListeners(ComponentProvider componentProvider) {
        final List<WsLifecycleListener> lifecycleListeners = componentProvider
            .getInstancesOf(WsLifecycleListener.class).stream()
            .sorted(Comparator.comparing(WsLifecycleListener::getPriority).reversed())
            .toList();
        if (log.isDebugEnabled()) {
            final String pipeline = lifecycleListeners.stream()
                .map(listener -> listener.getClass().getSimpleName())
                .collect(Collectors.joining(" -> "));
            log.debug("CompositeWsLifecycleListener initialized with pipeline: {}", pipeline);
        }
        log.info("Load {} WS lifecycle listener(s)", lifecycleListeners.size());
        return new CompositeWsLifecycleListener(lifecycleListeners);
    }

    private WsBroadcaster determinateWsBroadcaster(AbstractBuilder<?> builder) {
        WsMessageSink messageSink = localSessionDispatcher;
        if (builder.messageSink != null) {
            messageSink = builder.messageSink;
        }
        final EnvelopeSerializer<?> envelopeSerializer = serializerRegistry
            .get(EnvelopeSerializerFormat.from(StandardSerializerFormat.JSON,
                EnvelopeDataType.BINARY));
        return new WsMessageSinkBroadcaster(messageSink, envelopeSerializer);
    }

    public abstract int getLocalPort();

    protected abstract static class AbstractBuilder<B extends AbstractBuilder<B>> {
        private final List<WsMessageListener> messageListeners = new ArrayList<>();
        private final List<WsAuthenticator> authenticators = new ArrayList<>();

        private int port;
        private String path = "";
        private long maxMessageSize = 128 * 1024;
        private Duration idleTimeout = Duration.ofMinutes(10);
        private RateLimiter rateLimiter = new NoOpRateLimiter();
        private ComponentProvider componentProvider;
        private EnvelopeSerializerRegistry serializerRegistry;
        private WsSerializerResolverFactory serializerResolverFactory;
        private WsSessionRegistry sessionRegistry;
        private WsAuthFailureHandler authFailureHandler;
        private LocalSessionDispatcherFactory localSessionDispatcherFactory;
        private WsMessageSink messageSink;

        protected AbstractBuilder() {
        }

        protected B self() {
            return CastUtil.unsafeCast(this);
        }

        public B addMessageListener(WsMessageListener messageListener) {
            messageListeners.add(messageListener);
            return self();
        }

        public B addAuthenticator(WsAuthenticator authenticator) {
            authenticators.add(authenticator);
            return self();
        }

        public B port(int port) {
            this.port = port;
            return self();
        }

        public B path(String path) {
            this.path = path;
            return self();
        }

        public B maxMessageSize(long maxMessageSize) {
            this.maxMessageSize = maxMessageSize;
            return self();
        }

        public B idleTimeout(Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
            return self();
        }

        public B rateLimiter(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
            return self();
        }

        public B componentProvider(ComponentProvider componentProvider) {
            this.componentProvider = componentProvider;
            return self();
        }

        public B serializerRegistry(EnvelopeSerializerRegistry serializerRegistry) {
            this.serializerRegistry = serializerRegistry;
            return self();
        }

        public B serializerResolverFactory(WsSerializerResolverFactory factory) {
            serializerResolverFactory = factory;
            return self();
        }

        public B sessionRegistry(WsSessionRegistry sessionRegistry) {
            this.sessionRegistry = sessionRegistry;
            return self();
        }

        public B authFailureHandler(@Nullable WsAuthFailureHandler authFailureHandler) {
            this.authFailureHandler = authFailureHandler;
            return self();
        }

        public B localSessionDispatcherFactory(LocalSessionDispatcherFactory factory) {
            localSessionDispatcherFactory = factory;
            return self();
        }

        public B messageSink(@Nullable WsMessageSink messageSink) {
            this.messageSink = messageSink;
            return self();
        }

        protected void validate() {
            Assert.notEmpty(messageListeners, "WsMessageListeners cannot be empty");
            Assert.notNullAll(messageListeners, "All WsMessageListeners must be initialized");
            Assert.notNullAll(authenticators, "All WsAuthenticators must be initialized");
            Assert.state(port >= 0 && port < 65536, "Invalid port number");
            Assert.notNull(path, "Path cannot be null");
            Assert.notNull(idleTimeout, "IdleTimeout cannot be null");
            Assert.notNull(rateLimiter, "RateLimiter cannot be null");
            Assert.notNull(componentProvider, "ComponentProvider cannot be null");
            Assert.notNull(serializerRegistry, "EnvelopeSerializerRegistry cannot be null");
            Assert.notNull(serializerResolverFactory, "WsSerializerResolverFactory cannot be null");
            Assert.notNull(sessionRegistry, "WsSessionRegistry cannot be null");
            Assert.notNull(localSessionDispatcherFactory,
                "LocalSessionDispatcherFactory cannot be null");
        }

        public abstract WsServer build();
    }
}
