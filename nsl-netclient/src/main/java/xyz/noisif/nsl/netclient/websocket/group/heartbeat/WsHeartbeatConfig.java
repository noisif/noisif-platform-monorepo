/*
 * Copyright (c) 2022-2026 NOISIF. All Rights Reserved.
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
package xyz.noisif.nsl.netclient.websocket.group.heartbeat;

import xyz.noisif.nsl.common.util.Assert;

import java.time.Duration;

public class WsHeartbeatConfig {
  private final Duration interval;
  private final WsHeartbeatAction action;

  private WsHeartbeatConfig(Duration interval, WsHeartbeatAction action) {
    this.interval = Assert.notNullAndGet(interval, "Interval cannot be null");
    this.action = Assert.notNullAndGet(action, "Action cannot be null");
  }

  public static WsHeartbeatConfig create(Duration interval, WsHeartbeatAction action) {
    return new WsHeartbeatConfig(interval, action);
  }

  public static WsHeartbeatConfig createEnvelope(Duration interval) {
    return new WsHeartbeatConfig(interval, new EnvelopeHeartbeatAction());
  }

  public Duration getInterval() {
    return interval;
  }

  public WsHeartbeatAction getAction() {
    return action;
  }
}
