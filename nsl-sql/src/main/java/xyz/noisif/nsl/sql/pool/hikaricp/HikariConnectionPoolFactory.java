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
package xyz.noisif.nsl.sql.pool.hikaricp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.sql.config.SqlDatabaseConfig;
import xyz.noisif.nsl.sql.pool.ConnectionPoolFactory;
import xyz.noisif.nsl.sql.pool.ManagedDataSource;

public class HikariConnectionPoolFactory implements ConnectionPoolFactory {
  private static final Logger LOG = LoggerFactory.getLogger(HikariConnectionPoolFactory.class);

  private HikariConnectionPoolFactory() {}

  public static ConnectionPoolFactory create() {
    return new HikariConnectionPoolFactory();
  }

  @Override
  public ManagedDataSource createPool(SqlDatabaseConfig config) {
    final String jdbcUrl = config.buildJdbcUrl();
    final HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(jdbcUrl);
    hikariConfig.setUsername(config.username());
    hikariConfig.setPassword(config.password());

    hikariConfig.setMaximumPoolSize(config.maxPoolSize());
    hikariConfig.setMinimumIdle(2);
    hikariConfig.setConnectionTimeout(5000);
    hikariConfig.setIdleTimeout(600000); // 10 minutes
    hikariConfig.setMaxLifetime(1800000); // 30 minutes

    config.getDriverProperties().forEach(hikariConfig::addDataSourceProperty);

    final HikariDataSource pool = new HikariDataSource(hikariConfig);
    LOG.info("Created HikariCP pool for URL: {}", jdbcUrl);

    return new ManagedDataSource(pool, pool::close);
  }
}
