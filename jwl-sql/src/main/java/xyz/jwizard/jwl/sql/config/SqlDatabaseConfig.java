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
package xyz.jwizard.jwl.sql.config;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.net.HostPort;
import xyz.jwizard.jwl.net.NetworkUtil;

import java.util.Map;

public record SqlDatabaseConfig(
    SqlDatabaseDialect dialect,
    HostPort hostPort,
    String username,
    String password,
    String databaseName,
    int maxPoolSize) {
  public static Builder builder() {
    return new Builder();
  }

  public String buildJdbcUrl() {
    return dialect.buildJdbcUrl(hostPort, databaseName);
  }

  public Map<String, String> getDriverProperties() {
    return dialect.defaultDriverProperties();
  }

  public static class Builder {
    private SqlDatabaseDialect dialect;
    private HostPort hostPort;
    private String username;
    private String password;
    private String databaseName;
    private int maxPoolSize = 10;

    private Builder() {}

    public Builder dialect(SqlDatabaseDialect dialect) {
      this.dialect = dialect;
      return this;
    }

    public Builder dialect(String dialectRaw) {
      dialect = SqlDatabaseDialect.fromString(dialectRaw);
      return this;
    }

    public Builder hostPort(HostPort hostPort) {
      this.hostPort = hostPort;
      return this;
    }

    public Builder address(String address) {
      this.hostPort = NetworkUtil.parseHostPort(address);
      return this;
    }

    public Builder credentials(String username, String password) {
      this.username = username;
      this.password = password;
      return this;
    }

    public Builder databaseName(String databaseName) {
      this.databaseName = databaseName;
      return this;
    }

    public Builder maxPoolSize(int maxPoolSize) {
      this.maxPoolSize = maxPoolSize;
      return this;
    }

    public SqlDatabaseConfig build() {
      Assert.notNull(dialect, "Database dialect cannot be null");
      Assert.notNull(hostPort, "Database host/port cannot be null");
      Assert.notNull(username, "Database username cannot be null");
      Assert.notNull(databaseName, "Database name cannot be null");
      return new SqlDatabaseConfig(
          dialect, hostPort, username, password, databaseName, maxPoolSize);
    }
  }
}
