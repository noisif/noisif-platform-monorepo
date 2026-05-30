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
package xyz.jwizard.jwl.sql;

import xyz.jwizard.jwl.common.bootstrap.lifecycle.IdempotentService;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.sql.config.SqlDatabaseConfig;
import xyz.jwizard.jwl.sql.pool.ConnectionPoolFactory;
import xyz.jwizard.jwl.sql.pool.ManagedDataSource;

import javax.sql.DataSource;

public abstract class GenericSqlClient extends IdempotentService
    implements SqlClient, SqlClientLifecycle {
  private final SqlDatabaseConfig config;
  private final ConnectionPoolFactory poolFactory;

  private DataSource dataSource;
  private Runnable closeAction;

  protected GenericSqlClient(SqlDatabaseConfig config, ConnectionPoolFactory poolFactory) {
    this.config = config;
    this.poolFactory = poolFactory;
  }

  @Override
  protected final void onStart() {
    if (dataSource != null) {
      log.warn("Database client for '{}' is already started", config.databaseName());
      return;
    }
    log.info("Starting database connection pool for: {}", config.databaseName());
    final ManagedDataSource pool = poolFactory.createPool(config);
    dataSource = pool.dataSource();
    closeAction = pool.closeAction();
  }

  @Override
  protected final void onStop() {
    IoUtil.closeQuietly(closeAction);
  }

  protected DataSource getActiveDataSource() {
    if (dataSource == null) {
      throw new IllegalStateException("Database client is not started");
    }
    return dataSource;
  }
}
