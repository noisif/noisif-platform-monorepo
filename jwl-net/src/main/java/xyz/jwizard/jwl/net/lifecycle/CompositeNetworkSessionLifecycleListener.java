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
package xyz.jwizard.jwl.net.lifecycle;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.Ordered;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.reflect.TypeReference;
import xyz.jwizard.jwl.net.NetworkSession;

public class CompositeNetworkSessionLifecycleListener<S extends NetworkSession>
    implements NetworkSessionLifecycleListener<S> {
    private static final Logger LOG = LoggerFactory
        .getLogger(CompositeNetworkSessionLifecycleListener.class);

    private final List<NetworkSessionLifecycleListener<S>> lifecycleListeners;

    private CompositeNetworkSessionLifecycleListener(List<NetworkSessionLifecycleListener<S>>
                                                         lifecycleListeners) {
        this.lifecycleListeners = lifecycleListeners;
    }

    public static <S extends NetworkSession> NetworkSessionLifecycleListener<S> load(
        ComponentProvider componentProvider) {
        final List<NetworkSessionLifecycleListener<S>> lifecycleListeners = componentProvider
            .getInstancesOf(new TypeReference<NetworkSessionLifecycleListener<S>>() {
            }).stream()
            .sorted(Ordered.COMPARATOR)
            .toList();
        if (LOG.isDebugEnabled()) {
            final String pipeline = lifecycleListeners.stream()
                .map(listener -> listener.getClass().getSimpleName())
                .collect(Collectors.joining(" -> "));
            LOG.debug("CompositeLifecycleListener initialized with pipeline: {}",
                pipeline.isEmpty() ? "none" : pipeline);
        }
        LOG.info("Loaded {} lifecycle listener(s)", lifecycleListeners.size());
        return new CompositeNetworkSessionLifecycleListener<>(lifecycleListeners);
    }

    @Override
    public void onConnect(S session) {
        LOG.debug("Session {} connecting to pipeline ({} listeners)", session.getSessionId(),
            lifecycleListeners.size());
        for (final NetworkSessionLifecycleListener<S> listener : lifecycleListeners) {
            listener.onConnect(session);
        }
    }

    @Override
    public void onClose(S session, int statusCode, String reason) {
        LOG.debug("Session {} closing (code: {}, reason: {}), notifying pipeline",
            session.getSessionId(), statusCode, reason);
        for (final NetworkSessionLifecycleListener<S> listener : lifecycleListeners) {
            listener.onClose(session, statusCode, reason);
        }
    }

    @Override
    public void onError(S session, Throwable cause) {
        LOG.debug("Error in session {}: {}, notifying pipeline", session.getSessionId(),
            cause.getMessage());
        for (final NetworkSessionLifecycleListener<S> listener : lifecycleListeners) {
            listener.onError(session, cause);
        }
    }
}
