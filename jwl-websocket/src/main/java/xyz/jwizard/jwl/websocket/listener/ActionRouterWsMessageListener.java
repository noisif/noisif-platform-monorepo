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
package xyz.jwizard.jwl.websocket.listener;

import xyz.jwizard.jwl.codec.envelope.MessageEnvelope;
import xyz.jwizard.jwl.net.envelope.bus.EnvelopeBusListener;
import xyz.jwizard.jwl.websocket.WsSession;
import xyz.jwizard.jwl.websocket.listener.action.HeartbeatAction;
import xyz.jwizard.jwl.websocket.listener.action.WsOpCode;

public class ActionRouterWsMessageListener extends EnvelopeBusListener<WsSession> {
    private ActionRouterWsMessageListener(Builder builder) {
        super(builder);
        registerManually(new HeartbeatAction());
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void handleProcessUnknownAction(WsSession session, MessageEnvelope<?> envelope) {
        super.handleProcessUnknownAction(session, envelope);
        session.sendEnvelope(WsOpCode.UNKNOWN_ACTION);
    }

    @Override
    protected void handleProcessingError(WsSession session, Exception ex) {
        super.handleProcessingError(session, ex);
        session.sendEnvelope(WsOpCode.INVALID_PAYLOAD);
    }

    public static class Builder extends AbstractBuilder<WsSession, Builder> {
        protected Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public EnvelopeBusListener<WsSession> build() {
            super.validate();
            return new ActionRouterWsMessageListener(this);
        }
    }
}
