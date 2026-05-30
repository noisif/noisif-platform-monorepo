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
package xyz.jwizard.jwl.net.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.Ordered;
import xyz.jwizard.jwl.net.NetworkSession;

import java.util.List;
import java.util.stream.Collectors;

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
      final String pipeline =
          busListeners.stream()
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
      LOG.trace(
          "Propagating binary message ({} bytes) from session: {} through pipeline",
          message.length,
          channel.getSessionId());
    }
    for (final RawBusListener<S> entry : busEntries) {
      try {
        entry.dispatch(channel, message);
      } catch (Exception ex) {
        LOG.error(
            "Bus listener {} failed to process binary message",
            entry.getClass().getSimpleName(),
            ex);
      }
    }
  }

  @Override
  public final void dispatch(S channel, String message) {
    if (LOG.isTraceEnabled()) {
      LOG.trace(
          "Propagating text message (length: {}) from session: {} through pipeline",
          message.length(),
          channel.getSessionId());
    }
    for (final RawBusListener<S> entry : busEntries) {
      try {
        entry.dispatch(channel, message);
      } catch (Exception ex) {
        LOG.error(
            "Bus listener {} failed to process text message", entry.getClass().getSimpleName(), ex);
      }
    }
  }
}
