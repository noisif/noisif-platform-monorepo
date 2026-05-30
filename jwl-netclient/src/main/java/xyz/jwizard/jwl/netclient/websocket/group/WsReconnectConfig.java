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
package xyz.jwizard.jwl.netclient.websocket.group;

import xyz.jwizard.jwl.common.util.Assert;

import java.time.Duration;

public class WsReconnectConfig {
  private final boolean enabled;
  private final Duration delay;
  private final int maxAttempts;

  private WsReconnectConfig(boolean enabled, Duration delay, int maxAttempts) {
    this.enabled = enabled;
    this.delay = delay;
    this.maxAttempts = maxAttempts;
  }

  public static WsReconnectConfig disabled() {
    return new WsReconnectConfig(false, Duration.ZERO, 0);
  }

  public static WsReconnectConfig enabled(Duration delay, int maxAttempts) {
    Assert.notNull(delay, "Delay cannot be null");
    return new WsReconnectConfig(true, delay, maxAttempts);
  }

  public static WsReconnectConfig enabledInfinite(Duration delay) {
    Assert.notNull(delay, "Delay cannot be null");
    return new WsReconnectConfig(true, delay, -1);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Duration getDelay() {
    return delay;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }
}
