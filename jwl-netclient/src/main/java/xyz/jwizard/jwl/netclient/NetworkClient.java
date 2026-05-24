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
package xyz.jwizard.jwl.netclient;

import java.time.Duration;

import xyz.jwizard.jwl.common.bootstrap.lifecycle.IdempotentService;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.group.ClientGroupConfig;
import xyz.jwizard.jwl.netclient.group.ClientRegistry;
import xyz.jwizard.jwl.netclient.group.InMemoryClientRegistry;

public abstract class NetworkClient<T extends ClientGroupConfig> extends IdempotentService {
    protected final Duration connectTimeout;
    protected final ClientRegistry<T> clientsRegistry;

    protected NetworkClient(AbstractBaseBuilder<T, ?> builder) {
        connectTimeout = builder.connectTimeout;
        clientsRegistry = builder.clientsRegistry;
    }

    protected abstract static class AbstractBaseBuilder<T extends ClientGroupConfig,
        B extends AbstractBaseBuilder<T, B>> {
        private Duration connectTimeout = Duration.ofMinutes(1);
        private ClientRegistry<T> clientsRegistry = InMemoryClientRegistry.createDefault();

        protected AbstractBaseBuilder() {
        }

        protected abstract B self();

        public B connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return self();
        }

        public B clientsRegistry(ClientRegistry<T> clientsRegistry) {
            this.clientsRegistry = clientsRegistry;
            return self();
        }

        public B defaultClientGroup(T clientPool) {
            clientsRegistry.register(clientPool);
            return self();
        }

        public B clientGroup(ClientGroup clientGroup, T clientPool) {
            clientsRegistry.register(clientGroup, clientPool);
            return self();
        }

        protected void validate() {
            Assert.notNull(connectTimeout, "ConnectTimeout cannot be null");
            Assert.notNull(clientsRegistry, "ClientsRegistry cannot be null");
            Assert.notNullAll(clientsRegistry.getEntries(),
                "All ClientGroupConfigs must be initialized");
        }
    }
}
