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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.envelope.MessageEnvelope;
import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.reflect.TypeReference;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.common.util.CastUtil;
import xyz.jwizard.jwl.websocket.WsSession;
import xyz.jwizard.jwl.websocket.listener.WsMessageListener;

public class ActionRouterWsMessageListener extends WsMessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(ActionRouterWsMessageListener.class);

    private final Map<Integer, Class<?>> typeRegistry = new ConcurrentHashMap<>();
    private final Map<Integer, WsAction<?>> actionHandlers = new ConcurrentHashMap<>();

    private final ComponentProvider componentProvider;

    private ActionRouterWsMessageListener(Builder builder) {
        super(builder);
        componentProvider = builder.componentProvider;
        registerHeartbeatAction();
        registerWsActions();
    }

    public static Builder builder() {
        return new Builder();
    }

    private void registerHeartbeatAction() {
        final WsAction<Void> heartbeatAction = new HeartbeatAction();
        typeRegistry.put(heartbeatAction.opCode().getCode(), heartbeatAction.payloadClass());
        actionHandlers.put(heartbeatAction.opCode().getCode(), heartbeatAction);
        LOG.info("Successfully registered heartbeat WS action");
    }

    private void registerWsActions() {
        final Collection<WsAction<?>> actions = componentProvider
            .getInstancesOf(new TypeReference<>() {
            });
        int registeredCount = 0;
        for (final WsAction<?> action : actions) {
            if (pool == null || action.pool() == pool) {
                registerAction(action);
                registeredCount++;
            }
        }
        LOG.info("Successfully auto-registered {} WS action(s)", registeredCount);
    }

    private void registerAction(WsAction<?> action) {
        final OpCode op = action.opCode();
        typeRegistry.put(op.getCode(), action.payloadClass());
        actionHandlers.put(op.getCode(), action);
        LOG.debug("Registered action: {} (OP: {})", action.getClass().getSimpleName(),
            op.getCode());
    }

    @Override
    public void onMessage(WsSession session, byte[] message) {
        try {
            processEnvelope(session, session.unwrap(message, typeRegistry::get));
        } catch (Exception ex) {
            handleProcessingError(session, ex);
        }
    }

    @Override
    public void onMessage(WsSession session, String message) {
        try {
            processEnvelope(session, session.unwrap(message, typeRegistry::get));
        } catch (Exception ex) {
            handleProcessingError(session, ex);
        }
    }

    private void processEnvelope(WsSession session, MessageEnvelope<?> envelope) {
        final WsAction<Object> handler = CastUtil.unsafeCast(actionHandlers.get(envelope.op()));
        if (handler != null) {
            handler.handle(session, envelope.data());
        } else {
            LOG.warn("No action handler found for OP code: {} from session: {}",
                envelope.op(), session.getSessionId());
            session.sendEnvelope(WsOpCode.UNKNOWN_ACTION);
        }
    }

    private void handleProcessingError(WsSession session, Exception ex) {
        LOG.error("Error processing incoming WS message from {}: {}",
            session.getSessionId(), ex.getMessage());
        session.sendEnvelope(WsOpCode.INVALID_PAYLOAD);
    }

    public static class Builder extends AbstractBuilder<Builder> {
        private ComponentProvider componentProvider;

        private Builder() {
        }

        public Builder componentProvider(ComponentProvider componentProvider) {
            this.componentProvider = componentProvider;
            return this;
        }

        @Override
        public WsMessageListener build() {
            Assert.notNull(componentProvider, "ComponentProvider cannot be null");
            return new ActionRouterWsMessageListener(this);
        }
    }
}
