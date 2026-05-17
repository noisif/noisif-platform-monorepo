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
package xyz.jwizard.jwl.netclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.bootstrap.lifecycle.IdempotentService;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.group.ClientGroupConfig;
import xyz.jwizard.jwl.netclient.group.ClientRegistry;
import xyz.jwizard.jwl.netclient.group.InMemoryClientRegistry;

public abstract class NetworkClient<T extends ClientGroupConfig> extends IdempotentService {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ClientRegistry<T> clientsRegistry;

    protected NetworkClient(AbstractBaseBuilder<T, ?> builder) {
        clientsRegistry = builder.clientsRegistry;
    }

    protected abstract static class AbstractBaseBuilder<T extends ClientGroupConfig,
        B extends AbstractBaseBuilder<T, B>> {
        private ClientRegistry<T> clientsRegistry = InMemoryClientRegistry.createDefault();

        protected AbstractBaseBuilder() {
        }

        protected abstract B self();

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
            Assert.notNull(clientsRegistry, "ClientsRegistry cannot be null");
            Assert.notNullAll(clientsRegistry.getConfigs(),
                "All ClientGroupConfigs must be initialized");
        }
    }
}
