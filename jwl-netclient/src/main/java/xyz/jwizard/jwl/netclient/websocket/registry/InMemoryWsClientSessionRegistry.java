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
package xyz.jwizard.jwl.netclient.websocket.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import xyz.jwizard.jwl.common.registry.GenericConcurrentRegistry;
import xyz.jwizard.jwl.net.ws.WsCloseCode;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.websocket.WsClientSession;

// 1:1 socket <-> group relation
public class InMemoryWsClientSessionRegistry
    extends GenericConcurrentRegistry<String, WsClientSession> implements WsClientSessionRegistry {
    private InMemoryWsClientSessionRegistry() {
        super();
    }

    public static InMemoryWsClientSessionRegistry createDefault() {
        return new InMemoryWsClientSessionRegistry();
    }

    @Override
    public void register(WsClientSession session) {
        final ClientGroup group = session.getGroup();
        final WsClientSession oldSession = super.putDirect(group.getClientGroupName(), session);
        if (oldSession != null && !oldSession.isClosed()) {
            log.warn("Overwriting active session for group {}, closing the old one.",
                group.getClientGroupName());
            oldSession.close(WsCloseCode.REPLACED_SESSION);
        }
        log.debug("Registered session {} for group {}", session.getSessionId(),
            group.getClientGroupName());
    }

    @Override
    public void unregister(WsClientSession session) {
        if (super.removeDirect(session.getGroup().getClientGroupName(), session)) {
            log.debug("Unregistered session {}", session.getSessionId());
        }
    }

    @Override
    public Collection<WsClientSession> getSessions(ClientGroup clientGroup) {
        final WsClientSession session = super.getOrNull(clientGroup.getClientGroupName());
        return session != null ? Collections.singletonList(session) : List.of();
    }
}
