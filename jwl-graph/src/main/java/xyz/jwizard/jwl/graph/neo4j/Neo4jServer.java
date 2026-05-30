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
package xyz.jwizard.jwl.graph.neo4j;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.graph.GraphServer;
import xyz.jwizard.jwl.graph.neo4j.client.factory.Neo4jConfig;

public class Neo4jServer extends GraphServer<Neo4jConfig> {
  protected Neo4jServer(Builder builder) {
    super(builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends AbstractBuilder<Builder, Neo4jConfig> {
    private Builder() {}

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    protected void validate() {
      super.validate();
      Assert.notNull(config.getUsername(), "Username cannot be null");
      Assert.notNull(config.getPassword(), "Password cannot be null");
    }

    @Override
    public GraphServer<Neo4jConfig> build() {
      validate();
      return new Neo4jServer(this);
    }
  }
}
