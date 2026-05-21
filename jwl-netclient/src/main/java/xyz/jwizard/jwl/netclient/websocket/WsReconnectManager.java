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
package xyz.jwizard.jwl.netclient.websocket;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.net.CloseCode;
import xyz.jwizard.jwl.net.ws.WsCloseCode;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.websocket.group.WsClientGroupConfig;
import xyz.jwizard.jwl.netclient.websocket.group.WsReconnectConfig;

class WsReconnectManager {
    private static final Logger LOG = LoggerFactory.getLogger(WsReconnectManager.class);
    private static final Set<Integer> normalClosure = CloseCode.ofCodes(
        WsCloseCode.NORMAL
    );

    private final ScheduledExecutorService scheduler;

    WsReconnectManager(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    void handleDisconnect(ClientGroup group, WsClientGroupConfig config, int closeCode,
                          Runnable connectAction) {
        if (normalClosure.contains(closeCode)) {
            LOG.debug("Normal WS closure detected for group '{}', suppressing reconnect.",
                group.getClientGroupName());
            return;
        }
        scheduleReconnect(group, config, 1, connectAction);
    }

    void handleFailure(ClientGroup group, WsClientGroupConfig config, int currentAttempt,
                       Runnable connectAction) {
        scheduleReconnect(group, config, currentAttempt, connectAction);
    }

    private void scheduleReconnect(ClientGroup group, WsClientGroupConfig config, int attempt,
                                   Runnable connectAction) {
        final WsReconnectConfig reconnectConfig = config.getReconnectConfig();
        if (reconnectConfig == null || !reconnectConfig.isEnabled()) {
            LOG.trace("Reconnect policy disabled for group '{}', aborting",
                group.getClientGroupName());
            return;
        }
        if (reconnectConfig.getMaxAttempts() != -1 && attempt > reconnectConfig.getMaxAttempts()) {
            LOG.error("Max reconnect attempts ({}) reached for WS group '{}', giving up",
                reconnectConfig.getMaxAttempts(), group.getClientGroupName());
            return;
        }
        final long delayMs = reconnectConfig.getDelay().toMillis();
        LOG.info("Scheduling WS connection retry for group '{}' in {} ms (attempt: {})",
            group.getClientGroupName(), delayMs, attempt + 1);
        final ScheduledFuture<?> future = scheduler
            .schedule(connectAction, delayMs, TimeUnit.MILLISECONDS);
        LOG.debug("Task successfully scheduled for group '{}', is done: {}",
            group.getClientGroupName(), future.isDone());
    }
}
