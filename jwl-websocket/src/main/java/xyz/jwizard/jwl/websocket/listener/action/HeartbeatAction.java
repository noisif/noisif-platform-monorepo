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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.net.envelope.EnvelopeAction;
import xyz.jwizard.jwl.websocket.WsSession;

public class HeartbeatAction implements EnvelopeAction<WsSession, Void> {
    private static final Logger LOG = LoggerFactory.getLogger(HeartbeatAction.class);

    @Override
    public void handle(WsSession session, Void data) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Heartbeat received from session: {}", session.getSessionId());
        }
        session.sendEnvelope(WsOpCode.HEARTBEAT);
    }

    @Override
    public OpCode opCode() {
        return WsOpCode.HEARTBEAT;
    }

    @Override
    public Class<Void> payloadClass() {
        return Void.class;
    }
}
