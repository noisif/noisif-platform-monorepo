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
package xyz.jwizard.jwl.net.bus;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.Ordered;
import xyz.jwizard.jwl.net.NetworkSession;

public class CompositeBusListener<S extends NetworkSession> implements RawBusListener<S> {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeBusListener.class);

    private final List<RawBusListener<S>> busEntries;

    private CompositeBusListener(List<RawBusListener<S>> busEntries) {
        this.busEntries = busEntries;
    }

    public static <S extends NetworkSession> RawBusListener<S> load(
        List<RawBusListener<S>> busListeners) {
        busListeners.sort(Ordered.COMPARATOR);
        if (LOG.isDebugEnabled()) {
            final String pipeline = busListeners.stream()
                .map(listener -> listener.getClass().getSimpleName())
                .collect(Collectors.joining(" -> "));
            LOG.debug("CompositeBusListener initialized with pipeline: {}", pipeline);
        }
        LOG.info("Load {} bus listener(s)", busListeners.size());
        return new CompositeBusListener<>(busListeners);
    }

    @Override
    public final void dispatch(S channel, byte[] message) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Propagating binary message ({} bytes) from session: {} through pipeline",
                message.length, channel.getSessionId());
        }
        for (final RawBusListener<S> entry : busEntries) {
            try {
                entry.dispatch(channel, message);
            } catch (Exception ex) {
                LOG.error("Bus listener {} failed to process binary message",
                    entry.getClass().getSimpleName(), ex);
            }
        }
    }

    @Override
    public final void dispatch(S channel, String message) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Propagating text message (length: {}) from session: {} through pipeline",
                message.length(), channel.getSessionId());
        }
        for (final RawBusListener<S> entry : busEntries) {
            try {
                entry.dispatch(channel, message);
            } catch (Exception ex) {
                LOG.error("Bus listener {} failed to process text message",
                    entry.getClass().getSimpleName(), ex);
            }
        }
    }
}
