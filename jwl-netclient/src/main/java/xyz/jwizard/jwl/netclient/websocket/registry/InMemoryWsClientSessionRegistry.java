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
