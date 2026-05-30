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
package xyz.jwizard.jwl.websocket.listener.action;

import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.net.envelope.EnvelopeAction;
import xyz.jwizard.jwl.websocket.WsSession;
import xyz.jwizard.jwl.websocket.broadcast.TestWsTopic;
import xyz.jwizard.jwl.websocket.registry.WsSubscriptionRegistry;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
class SubscribeAction implements EnvelopeAction<WsSession, Void> {
  private final WsSubscriptionRegistry registry;

  @Inject
  SubscribeAction(WsSubscriptionRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void handle(WsSession session, Void data) {
    registry.subscribe(session, TestWsTopic.CHAT_ROOM);
    session.sendEnvelope(TestOpCode.SUBSCRIBE_ACK, session.getPrincipalId());
  }

  @Override
  public OpCode opCode() {
    return TestOpCode.SUBSCRIBE;
  }

  @Override
  public Class<Void> payloadClass() {
    return Void.class;
  }
}
