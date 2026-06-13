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
package xyz.noisif.nsl.kv;

import org.jspecify.annotations.Nullable;

import xyz.noisif.nsl.common.bootstrap.lifecycle.IdempotentService;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.reflect.TypeReference;
import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.common.util.StringUtil;
import xyz.noisif.nsl.kv.pubsub.PubSubBroadcaster;
import xyz.noisif.nsl.kv.pubsub.PubSubRegistrar;
import xyz.noisif.nsl.kv.pubsub.subscriber.KvSubscriber;
import xyz.noisif.nsl.kv.pubsub.subscriber.SubscriptionMode;
import xyz.noisif.nsl.net.HostPort;
import xyz.noisif.nsl.net.NetworkUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class KvServer extends IdempotentService
    implements KeyValueStore, PubSubBroadcaster {
  protected final Set<HostPort> nodes;
  protected final String password;
  protected final ComponentProvider componentProvider;

  private final Collection<KvSubscriber<?>> registeredSubscribers = new CopyOnWriteArrayList<>();

  protected KvServer(AbstractBuilder<?> builder) {
    nodes = builder.nodes;
    password = builder.password;
    componentProvider = builder.componentProvider;
  }

  public void awaitSubscribers(long timeoutMs) {
    final long limit = System.currentTimeMillis() + timeoutMs;
    for (final KvSubscriber<?> sub : registeredSubscribers) {
      while (!sub.isSubscribed()) {
        if (System.currentTimeMillis() > limit) {
          throw new RuntimeException(
              "Timeout waiting for subscriber readiness: " + sub.getClass().getSimpleName());
        }
        Thread.onSpinWait();
      }
    }
    log.info("All {} subscribers confirmed readiness", registeredSubscribers.size());
  }

  @Override
  protected final void onStart() {
    if (nodes.isEmpty()) {
      log.warn("Not providing any nodes, skipping configuration");
      return;
    }
    log.info("KV server start initializing with {} node(s)", nodes.size());
    onKvServerStart();
    final PubSubRegistrar registrar = createRegistrar();
    final int stringCount =
        registerSet(
            String.class, new TypeReference<>() {}, registrar::subscribe, registrar::pSubscribe);
    final int binaryCount =
        registerSet(
            byte[].class,
            new TypeReference<>() {},
            registrar::subscribeBinary,
            registrar::pSubscribeBinary);
    log.info(
        "KV subscribers auto-discovery completed, total registered: {}", stringCount + binaryCount);
  }

  protected abstract void onKvServerStart();

  protected abstract PubSubRegistrar createRegistrar();

  private <T> int registerSet(
      Class<T> payloadClass,
      TypeReference<KvSubscriber<T>> typeRef,
      Consumer<KvSubscriber<T>> exactRegistrar,
      Consumer<KvSubscriber<T>> patternRegistrar) {
    final Collection<KvSubscriber<T>> subscribers = componentProvider.getInstancesOf(typeRef);
    final Map<SubscriptionMode, Consumer<KvSubscriber<T>>> strategies =
        Map.of(
            SubscriptionMode.EXACT, exactRegistrar,
            SubscriptionMode.PATTERN, patternRegistrar);
    final List<KvSubscriber<T>> validSubscribers =
        subscribers.stream().filter(sub -> sub.getPayloadType().equals(payloadClass)).toList();
    for (final KvSubscriber<T> sub : validSubscribers) {
      final String channelStr = sub.getChannel().buildChannel(sub.getChannelParams());
      final SubscriptionMode mode = sub.getMode();

      registeredSubscribers.add(sub);
      strategies.get(mode).accept(sub);

      log.debug(
          "Auto-registered {} subscriber: [{}] on '{}'",
          StringUtil.toLowerCase(mode.name()),
          sub.getClass().getSimpleName(),
          channelStr);
    }
    if (!validSubscribers.isEmpty()) {
      log.info(
          "Successfully registered {} {} subscribers",
          validSubscribers.size(),
          payloadClass.getSimpleName());
    }
    return validSubscribers.size();
  }

  protected abstract static class AbstractBuilder<B extends AbstractBuilder<B>> {
    private Set<HostPort> nodes = new HashSet<>();
    private String password;
    private ComponentProvider componentProvider;

    protected AbstractBuilder() {}

    protected abstract B self();

    public B nodes(Set<HostPort> nodes) {
      this.nodes = nodes;
      return self();
    }

    // as host:port
    public B rawNodes(Set<String> rawNodes) {
      return nodes(
          rawNodes.stream()
              .map(NetworkUtil::parseHostPort)
              .map(hp -> HostPort.from(hp.host(), hp.port()))
              .collect(Collectors.toSet()));
    }

    public B password(@Nullable String password) {
      this.password = password;
      return self();
    }

    public B componentProvider(ComponentProvider componentProvider) {
      this.componentProvider = componentProvider;
      return self();
    }

    protected void validate() {
      Assert.notNull(nodes, "Nodes cannot be null");
      Assert.notNull(componentProvider, "ComponentProvider cannot be null");
    }

    public abstract KvServer build();
  }
}
