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
package xyz.jwizard.jwl.graph;

import xyz.jwizard.jwl.common.bootstrap.lifecycle.IdempotentService;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.graph.client.GraphClient;
import xyz.jwizard.jwl.graph.client.factory.GraphClientFactory;
import xyz.jwizard.jwl.graph.client.factory.GraphConfig;
import xyz.jwizard.jwl.graph.repository.GraphRepository;
import xyz.jwizard.jwl.net.NetworkUtil;

import java.net.URI;
import java.util.function.Function;

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

  protected abstract static class AbstractBuilder<
      B extends AbstractBuilder<B, C>, C extends GraphConfig> {
    protected C config;
    private URI uri;
    private GraphClientFactory<C> clientFactory;
    private Function<GraphClient, GraphRepository> repositoryFactory;

    protected AbstractBuilder() {}

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
