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
package xyz.jwizard.jwl.netclient.websocket.group;

import java.time.Duration;

import xyz.jwizard.jwl.common.util.Assert;

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
