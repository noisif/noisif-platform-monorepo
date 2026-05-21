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
package xyz.jwizard.jwl.netclient.websocket.group.heartbeat;

import java.time.Duration;

import xyz.jwizard.jwl.common.util.Assert;

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
