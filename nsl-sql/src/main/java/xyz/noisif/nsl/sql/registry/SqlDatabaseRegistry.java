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
package xyz.noisif.nsl.sql.registry;

import xyz.noisif.nsl.common.registry.GenericConcurrentRegistry;
import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.sql.GenericSqlClient;
import xyz.noisif.nsl.sql.SqlClient;
import xyz.noisif.nsl.sql.SqlClientFactory;
import xyz.noisif.nsl.sql.SqlClientLifecycle;
import xyz.noisif.nsl.sql.config.SqlDatabaseConfig;
import xyz.noisif.nsl.sql.pool.ConnectionPoolFactory;

import java.util.ArrayList;
import java.util.List;

public class SqlDatabaseRegistry extends GenericConcurrentRegistry<String, GenericSqlClient> {
  private SqlDatabaseRegistry(
      List<SqlRegistryConfig> sqlRegistryConfigs, ConnectionPoolFactory poolFactory) {
    super();
    registerFromConfigs(sqlRegistryConfigs, poolFactory);
  }

  public static Builder builder() {
    return new Builder();
  }

  // return simple SqlClient for hide lifecycle methods
  public SqlClient getClient(String databaseName) {
    return super.get(databaseName);
  }

  public void startAll() {
    for (final SqlClientLifecycle client : super.getAll()) {
      client.start();
    }
    log.info("Started {} SQL client(s)", super.getEntries().size());
  }

  public void closeAll() {
    for (final SqlClientLifecycle client : super.getAll()) {
      client.close();
    }
    log.info("Closed {} SQL client(s)", super.getEntries().size());
    super.clear();
  }

  private void registerFromConfigs(
      List<SqlRegistryConfig> sqlRegistryConfigs, ConnectionPoolFactory poolFactory) {
    for (final SqlRegistryConfig config : sqlRegistryConfigs) {
      final String dbName = config.config().databaseName();
      log.debug("Building SQL client for database: '{}'", dbName);
      final GenericSqlClient sqlClient = config.factory().create(config.config(), poolFactory);
      register(dbName, sqlClient);
    }
    log.info("Registered and built {} database definition(s), not yet started", getAll().size());
  }

  public static class Builder {
    private final List<SqlRegistryConfig> sqlRegistryConfigs = new ArrayList<>();
    private ConnectionPoolFactory poolFactory;

    private Builder() {}

    public Builder poolFactory(ConnectionPoolFactory poolFactory) {
      this.poolFactory = poolFactory;
      return this;
    }

    public Builder register(SqlDatabaseConfig config, SqlClientFactory factory) {
      sqlRegistryConfigs.add(new SqlRegistryConfig(config, factory));
      return this;
    }

    public SqlDatabaseRegistry build() {
      Assert.notNull(poolFactory, "poolFactory");
      return new SqlDatabaseRegistry(sqlRegistryConfigs, poolFactory);
    }
  }
}
