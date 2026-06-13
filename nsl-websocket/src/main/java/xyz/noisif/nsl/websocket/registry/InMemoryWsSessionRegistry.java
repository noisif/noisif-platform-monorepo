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
package xyz.noisif.nsl.websocket.registry;

import xyz.noisif.nsl.common.registry.GenericConcurrentRegistry;
import xyz.noisif.nsl.websocket.WsSession;
import xyz.noisif.nsl.websocket.broadcast.WsTopic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryWsSessionRegistry extends GenericConcurrentRegistry<String, WsSession>
    implements WsSessionRegistry {
  // for optimization
  private final Map<String, Map<String, WsSession>> subscriptions = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> sessionTopics = new ConcurrentHashMap<>();

  private InMemoryWsSessionRegistry() {
    super();
  }

  public static InMemoryWsSessionRegistry createDefault() {
    return new InMemoryWsSessionRegistry();
  }

  @Override
  public void register(WsSession session) {
    super.register(session.getSessionId(), session);
    log.debug(
        "Session registered: {} (total active: {})", session.getSessionId(), getEntries().size());
  }

  @Override
  public void unregister(WsSession session) {
    final String sid = session.getSessionId();
    super.remove(sid);
    final Set<String> topics = sessionTopics.remove(sid);
    final int topicsCount = (topics != null) ? topics.size() : 0;
    if (topics == null) {
      return;
    }
    for (final String topic : topics) {
      subscriptions.computeIfPresent(
          topic,
          (t, subs) -> {
            subs.remove(sid);
            if (subs.isEmpty()) {
              log.trace("Topic '{}' became empty and was removed during unregister of {}", t, sid);
              return null;
            }
            return subs;
          });
    }
    log.debug(
        "Session unregistered: {} (was in {} topics, remaining sessions: {})",
        sid,
        topicsCount,
        getEntries().size());
  }

  @Override
  public Collection<WsSession> getUnsafeSubscribers(String topic) {
    final Map<String, WsSession> subs = subscriptions.get(topic);
    return (subs == null) ? List.of() : subs.values();
  }

  @Override
  public void subscribe(WsSession session, WsTopic topic) {
    final String sid = session.getSessionId();
    final String topicName = topic.getTopic();
    subscriptions
        .computeIfAbsent(
            topicName,
            k -> {
              log.trace("Creating new topic bucket: {}", k);
              return new ConcurrentHashMap<>();
            })
        .put(sid, session);
    sessionTopics.computeIfAbsent(sid, k -> ConcurrentHashMap.newKeySet()).add(topicName);
    log.debug("Session {} subscribed to '{}'", sid, topicName);
  }

  @Override
  public void unsubscribe(WsSession session, WsTopic topic) {
    final String sid = session.getSessionId();
    final String topicName = topic.getTopic();
    subscriptions.computeIfPresent(
        topicName,
        (t, subs) -> {
          subs.remove(sid);
          if (subs.isEmpty()) {
            log.trace("Topic '{}' removed because last subscriber {} left", t, sid);
            return null;
          }
          return subs;
        });
    sessionTopics.computeIfPresent(
        sid,
        (s, topics) -> {
          topics.remove(topicName);
          return topics.isEmpty() ? null : topics;
        });
    log.debug("Session {} unsubscribed from '{}'", sid, topicName);
  }

  @Override
  public Collection<WsSession> getSubscribers(WsTopic topic) {
    return getUnsafeSubscribers(topic.getTopic());
  }

  @Override
  public Collection<WsSession> getAllSessions() {
    return super.getAll();
  }
}
