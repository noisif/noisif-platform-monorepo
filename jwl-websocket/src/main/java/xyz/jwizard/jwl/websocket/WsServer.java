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
package xyz.jwizard.jwl.websocket;

import org.jspecify.annotations.Nullable;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializerRegistry;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;
import xyz.jwizard.jwl.codec.serialization.TypedSerializerFormat;
import xyz.jwizard.jwl.common.bootstrap.lifecycle.IdempotentService;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.limit.NoOpRateLimiter;
import xyz.jwizard.jwl.common.limit.RateLimiter;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.net.bus.CompositeBusListener;
import xyz.jwizard.jwl.net.bus.RawBusListener;
import xyz.jwizard.jwl.net.lifecycle.CompositeNetworkSessionLifecycleListener;
import xyz.jwizard.jwl.net.lifecycle.NetworkSessionLifecycleListener;
import xyz.jwizard.jwl.websocket.auth.CompositeWsAuthenticator;
import xyz.jwizard.jwl.websocket.auth.WsAuthenticator;
import xyz.jwizard.jwl.websocket.auth.handler.WsAuthFailureHandler;
import xyz.jwizard.jwl.websocket.broadcast.WsBroadcaster;
import xyz.jwizard.jwl.websocket.broadcast.WsMessageSink;
import xyz.jwizard.jwl.websocket.broadcast.WsMessageSinkBroadcaster;
import xyz.jwizard.jwl.websocket.dispatcher.LocalSessionDispatcher;
import xyz.jwizard.jwl.websocket.dispatcher.factory.LocalSessionDispatcherFactory;
import xyz.jwizard.jwl.websocket.negotation.WsSerializerResolver;
import xyz.jwizard.jwl.websocket.negotation.WsSerializerResolverFactory;
import xyz.jwizard.jwl.websocket.registry.WsSessionRegistry;
import xyz.jwizard.jwl.websocket.registry.WsSubscriptionRegistry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
  protected final NetworkSessionLifecycleListener<WsSession> lifecycleListener;
  protected final RawBusListener<WsSession> busListener;
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
    busListener = CompositeBusListener.load(builder.busListeners);
    authenticator = loadWsAuthenticators(builder.componentProvider, builder.authenticators);
    lifecycleListener = CompositeNetworkSessionLifecycleListener.load(builder.componentProvider);
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

  private WsAuthenticator loadWsAuthenticators(
      ComponentProvider componentProvider, List<WsAuthenticator> authenticators) {
    final Collection<WsAuthenticator> reflectAuthenticators =
        componentProvider.getInstancesOf(WsAuthenticator.class);
    reflectAuthenticators.addAll(authenticators);
    final List<WsAuthenticator> sortedAuthenticators =
        new ArrayList<>(reflectAuthenticators).stream().sorted(WsAuthenticator.COMPARATOR).toList();
    if (log.isDebugEnabled()) {
      final String pipeline =
          sortedAuthenticators.stream()
              .map(listener -> listener.getClass().getSimpleName())
              .collect(Collectors.joining(" -> "));
      log.debug("CompositeWsAuthenticator initialized with pipeline: {}", pipeline);
    }
    log.info(
        "Load {} ({} via reflection) WS authenticator(s)",
        sortedAuthenticators.size(),
        reflectAuthenticators.size());
    return new CompositeWsAuthenticator(sortedAuthenticators);
  }

  private WsBroadcaster determinateWsBroadcaster(AbstractBuilder<?> builder) {
    WsMessageSink messageSink = localSessionDispatcher;
    if (builder.messageSink != null) {
      messageSink = builder.messageSink;
    }
    final EnvelopeSerializer<?> envelopeSerializer =
        serializerRegistry.get(
            TypedSerializerFormat.from(StandardSerializerFormat.JSON, DataType.BINARY));
    return new WsMessageSinkBroadcaster(messageSink, envelopeSerializer);
  }

  public abstract int getLocalPort();

  protected abstract static class AbstractBuilder<B extends AbstractBuilder<B>> {
    private final List<RawBusListener<WsSession>> busListeners = new ArrayList<>();
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

    protected AbstractBuilder() {}

    protected abstract B self();

    public B addBusListener(RawBusListener<WsSession> busListener) {
      busListeners.add(busListener);
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
      Assert.notEmpty(busListeners, "EnvelopeBusListeners cannot be empty");
      Assert.notNullAll(busListeners, "All EnvelopeBusListeners must be initialized");
      Assert.notNullAll(authenticators, "All WsAuthenticators must be initialized");
      Assert.state(port >= 0 && port < 65536, "Invalid port number");
      Assert.notNull(path, "Path cannot be null");
      Assert.notNull(idleTimeout, "IdleTimeout cannot be null");
      Assert.notNull(rateLimiter, "RateLimiter cannot be null");
      Assert.notNull(componentProvider, "ComponentProvider cannot be null");
      Assert.notNull(serializerRegistry, "EnvelopeSerializerRegistry cannot be null");
      Assert.notNull(serializerResolverFactory, "WsSerializerResolverFactory cannot be null");
      Assert.notNull(sessionRegistry, "WsSessionRegistry cannot be null");
      Assert.notNull(localSessionDispatcherFactory, "LocalSessionDispatcherFactory cannot be null");
    }

    public abstract WsServer build();
  }
}
