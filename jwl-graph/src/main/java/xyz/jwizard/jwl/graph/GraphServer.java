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
package xyz.jwizard.jwl.graph;

import java.net.URI;
import java.util.function.Function;

import xyz.jwizard.jwl.common.bootstrap.lifecycle.IdempotentService;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.graph.client.GraphClient;
import xyz.jwizard.jwl.graph.client.factory.GraphClientFactory;
import xyz.jwizard.jwl.graph.client.factory.GraphConfig;
import xyz.jwizard.jwl.graph.repository.GraphRepository;
import xyz.jwizard.jwl.net.NetworkUtil;

public abstract class GraphServer<C extends GraphConfig> extends IdempotentService {
    protected final URI uri;
    protected final C config;
    protected final GraphClientFactory<C> clientFactory;
    protected final Function<GraphClient, GraphRepository> repositoryFactory;

    protected GraphClient graphClient;
    protected GraphRepository graphRepository;

    protected GraphServer(AbstractBuilder<?, C> builder) {
        uri = builder.uri;
        config = builder.config;
        clientFactory = builder.clientFactory;
        repositoryFactory = builder.repositoryFactory;
    }

    @Override
    protected final void onStart() {
        log.info("Connecting to graph server on: {}", uri.toString());
        graphClient = clientFactory.createAndInitClient(config);
        graphRepository = repositoryFactory.apply(graphClient);
    }

    @Override
    protected final void onStop() {
        IoUtil.closeQuietly(graphClient);
    }

    public GraphClient getClient() {
        if (graphClient == null) {
            throw new IllegalStateException("Server not started");
        }
        return graphClient;
    }

    public GraphRepository getRepository() {
        if (graphRepository == null) {
            throw new IllegalStateException("Server not started");
        }
        return graphRepository;
    }

    protected static abstract class AbstractBuilder<B extends AbstractBuilder<B, C>,
        C extends GraphConfig> {
        protected C config;
        private URI uri;
        private GraphClientFactory<C> clientFactory;
        private Function<GraphClient, GraphRepository> repositoryFactory;

        protected AbstractBuilder() {
        }

        protected abstract B self();

        public B config(C config) {
            this.config = config;
            this.uri = NetworkUtil.parseToUri(config.getProtocol(), config.getAddress());
            return self();
        }

        public B clientFactory(GraphClientFactory<C> clientFactory) {
            this.clientFactory = clientFactory;
            return self();
        }

        public B repositoryFactory(Function<GraphClient, GraphRepository> repositoryFactory) {
            this.repositoryFactory = repositoryFactory;
            return self();
        }

        protected void validate() {
            Assert.notNull(config, "Config cannot be null");
            Assert.notNull(clientFactory, "ClientFactory cannot be null");
            Assert.notNull(repositoryFactory, "RepositoryFactory cannot be null");
        }

        public abstract GraphServer<C> build();
    }
}
