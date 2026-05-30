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
package xyz.jwizard.jwl.netclient.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.net.lifecycle.NetworkSessionLifecycleListener;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.websocket.group.WsClientGroupConfig;
import xyz.jwizard.jwl.netclient.websocket.heartbeat.WsHeartbeatManager;

class ManagedSessionLifecycleListener implements NetworkSessionLifecycleListener<WsClientSession> {
  private static final Logger LOG = LoggerFactory.getLogger(ManagedSessionLifecycleListener.class);

  private final ClientGroup clientGroup;
  private final WsClientGroupConfig config;
  private final WsHeartbeatManager heartbeatManager;
  private final WsReconnectManager reconnectManager;
  private final Runnable reconnectTrigger;

  ManagedSessionLifecycleListener(
      ClientGroup clientGroup,
      WsClientGroupConfig config,
      WsHeartbeatManager heartbeatManager,
      WsReconnectManager reconnectManager,
      Runnable reconnectTrigger) {
    this.clientGroup = clientGroup;
    this.config = config;
    this.heartbeatManager = heartbeatManager;
    this.reconnectManager = reconnectManager;
    this.reconnectTrigger = reconnectTrigger;
  }

  @Override
  public void onConnect(WsClientSession session) {
    LOG.info(
        "WS connected for group '{}'. session id: {}",
        clientGroup.getClientGroupName(),
        session.getSessionId());
    heartbeatManager.start(session, config.getHeartbeatConfig());
    config.getLifecycleListener().onConnect(session);
  }

  @Override
  public void onClose(WsClientSession session, int statusCode, String reason) {
    LOG.info(
        "WS closed for group '{}' [code: {}], session id: {}",
        clientGroup.getClientGroupName(),
        statusCode,
        session.getSessionId());
    heartbeatManager.stop(session.getSessionId());
    config.getLifecycleListener().onClose(session, statusCode, reason);
    reconnectManager.handleDisconnect(clientGroup, config, statusCode, reconnectTrigger);
  }

  @Override
  public void onError(WsClientSession session, Throwable cause) {
    LOG.warn("WS error for group '{}': {}", clientGroup.getClientGroupName(), cause.getMessage());
    config.getLifecycleListener().onError(session, cause);
  }
}
