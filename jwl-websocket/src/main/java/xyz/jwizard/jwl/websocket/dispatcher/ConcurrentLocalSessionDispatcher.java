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
package xyz.jwizard.jwl.websocket.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.websocket.WsSession;
import xyz.jwizard.jwl.websocket.registry.WsSessionRegistry;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrentLocalSessionDispatcher implements LocalSessionDispatcher {
  private static final Logger LOG = LoggerFactory.getLogger(ConcurrentLocalSessionDispatcher.class);

  private final WsSessionRegistry registry;
  private final ExecutorService executorService;

  private ConcurrentLocalSessionDispatcher(
      WsSessionRegistry registry, ExecutorService executorService) {
    this.registry = registry;
    this.executorService = executorService;
  }

  public static ConcurrentLocalSessionDispatcher createVirtual(WsSessionRegistry registry) {
    return new ConcurrentLocalSessionDispatcher(
        registry, Executors.newVirtualThreadPerTaskExecutor());
  }

  @Override
  public void dispatchRaw(String topic, byte[] payload) {
    broadcast(registry.getUnsafeSubscribers(topic), topic, payload);
  }

  @Override
  public void dispatchRawAll(byte[] payload) {
    broadcast(registry.getAllSessions(), null, payload);
  }

  private void broadcast(Collection<WsSession> sessions, String topic, byte[] payload) {
    if (sessions.isEmpty()) {
      return;
    }
    if (LOG.isTraceEnabled()) {
      LOG.trace(
          "Broadcasting RAW payload to {} sessions (topic: {})",
          sessions.size(),
          topic != null ? topic : "GLOBAL");
    }
    for (final WsSession session : sessions) {
      executorService.execute(() -> send(session, payload));
    }
  }

  private void send(WsSession session, byte[] payload) {
    if (session.isClosed()) {
      return;
    }
    try {
      session.sendAdapted(payload);
    } catch (Exception ex) {
      LOG.warn(
          "Send failed for session {}, removing. Reason: {}",
          session.getSessionId(),
          ex.getMessage());
      registry.unregister(session);
    }
  }
}
