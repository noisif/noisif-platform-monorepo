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
import java.util.Optional;

import xyz.jwizard.jwl.common.registry.RegistryTracker;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.websocket.WsClientSession;

public interface WsClientSessionRegistry extends RegistryTracker<WsClientSession> {
    // for 1:N
    Collection<WsClientSession> getSessions(ClientGroup clientGroup);

    default Optional<WsClientSession> getAnySession(ClientGroup clientGroup) {
        return getSessions(clientGroup).stream().findFirst();
    }
}
