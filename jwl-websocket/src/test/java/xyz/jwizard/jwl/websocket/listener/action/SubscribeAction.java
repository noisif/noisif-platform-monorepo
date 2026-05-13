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
