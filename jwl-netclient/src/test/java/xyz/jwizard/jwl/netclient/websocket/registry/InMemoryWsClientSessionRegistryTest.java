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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.net.ws.WsCloseCode;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.websocket.WsClientSession;

class InMemoryWsClientSessionRegistryTest {
  @Test
  @DisplayName("should close old session when new one registers for same group")
  void shouldCloseOldSessionWhenNewOneRegistersForSameGroup() {
    // given
    final InMemoryWsClientSessionRegistry registry =
        InMemoryWsClientSessionRegistry.createDefault();
    ClientGroup group = mock(ClientGroup.class);
    when(group.getClientGroupName()).thenReturn("group-1");
    final WsClientSession oldSession = mock(WsClientSession.class);
    when(oldSession.getGroup()).thenReturn(group);
    when(oldSession.isClosed()).thenReturn(false);
    final WsClientSession newSession = mock(WsClientSession.class);
    when(newSession.getGroup()).thenReturn(group);
    registry.register(oldSession);
    // when
    registry.register(newSession);
    // then
    verify(oldSession).close(WsCloseCode.REPLACED_SESSION);
    assertThat(registry.getSessions(group)).containsExactly(newSession);
  }
}
