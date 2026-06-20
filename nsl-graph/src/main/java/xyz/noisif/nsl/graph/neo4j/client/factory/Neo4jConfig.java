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
package xyz.noisif.nsl.graph.neo4j.client.factory;

import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.graph.client.factory.GraphConfig;

public class Neo4jConfig extends GraphConfig {
  private final String username;
  private final String password;

  protected Neo4jConfig(Builder builder) {
    super(builder);
    this.username = builder.username;
    this.password = builder.password;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public static final class Builder extends AbstractBuilder<Builder, Neo4jConfig> {
    private String username;
    private String password;

    private Builder() {}

    @Override
    protected Builder self() {
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return self();
    }

    public Builder password(String password) {
      this.password = password;
      return self();
    }

    @Override
    protected void validate() {
      super.validate();
      Assert.notNull(username, "username");
      Assert.notNull(password, "password");
    }

    @Override
    public Neo4jConfig build() {
      validate();
      return new Neo4jConfig(this);
    }
  }
}
