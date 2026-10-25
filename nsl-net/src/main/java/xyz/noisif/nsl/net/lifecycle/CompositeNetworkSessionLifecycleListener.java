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
package xyz.noisif.nsl.net.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.Ordered;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.reflect.TypeReference;
import xyz.noisif.nsl.net.NetworkSession;

import java.util.List;
import java.util.stream.Collectors;

public class CompositeNetworkSessionLifecycleListener<S extends NetworkSession>
    implements NetworkSessionLifecycleListener<S> {
  private static final Logger LOG =
      LoggerFactory.getLogger(CompositeNetworkSessionLifecycleListener.class);

  private final List<NetworkSessionLifecycleListener<S>> lifecycleListeners;

  private CompositeNetworkSessionLifecycleListener(
      List<NetworkSessionLifecycleListener<S>> lifecycleListeners) {
    this.lifecycleListeners = lifecycleListeners;
  }

  public static <S extends NetworkSession> NetworkSessionLifecycleListener<S> load(
      ComponentProvider componentProvider) {
    final List<NetworkSessionLifecycleListener<S>> lifecycleListeners =
        componentProvider
            .getInstancesOf(new TypeReference<NetworkSessionLifecycleListener<S>>() {})
            .stream()
            .sorted(Ordered.COMPARATOR)
            .toList();
    if (LOG.isDebugEnabled()) {
      final String pipeline =
          lifecycleListeners.stream()
              .map(listener -> listener.getClass().getSimpleName())
              .collect(Collectors.joining(" -> "));
      LOG.debug(
          "CompositeLifecycleListener initialized with pipeline: {}",
          pipeline.isEmpty() ? "none" : pipeline);
    }
    LOG.info("Loaded {} lifecycle listener(s)", lifecycleListeners.size());
    return new CompositeNetworkSessionLifecycleListener<>(lifecycleListeners);
  }

  @Override
  public void onConnect(S session) {
    LOG.debug(
        "Session {} connecting to pipeline ({} listeners)",
        session.getSessionId(),
        lifecycleListeners.size());
    for (final NetworkSessionLifecycleListener<S> listener : lifecycleListeners) {
      listener.onConnect(session);
    }
  }

  @Override
  public void onClose(S session, int statusCode, String reason) {
    LOG.debug(
        "Session {} closing (code: {}, reason: {}), notifying pipeline",
        session.getSessionId(),
        statusCode,
        reason);
    for (final NetworkSessionLifecycleListener<S> listener : lifecycleListeners) {
      listener.onClose(session, statusCode, reason);
    }
  }

  @Override
  public void onError(S session, Throwable cause) {
    LOG.debug(
        "Error in session {}: {}, notifying pipeline", session.getSessionId(), cause.getMessage());
    for (final NetworkSessionLifecycleListener<S> listener : lifecycleListeners) {
      listener.onError(session, cause);
    }
  }
}
