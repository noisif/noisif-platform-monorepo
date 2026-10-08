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
package xyz.noisif.nss.api.sql;

import xyz.noisif.nsl.sql.config.SqlDatabaseConfig;
import xyz.noisif.nsl.sql.config.SqlDatabaseDialect;
import xyz.noisif.nsl.sql.jdbc.JdbcSqlClient;
import xyz.noisif.nsl.sql.pool.hikaricp.HikariConnectionPoolFactory;
import xyz.noisif.nsl.sql.registry.SqlDatabaseRegistry;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
class SqlClientConfiguration {
  @Produces
  @Singleton
  SqlDatabaseRegistry sqlDatabaseRegistry() {
    return SqlDatabaseRegistry.builder()
        .poolFactory(HikariConnectionPoolFactory.create())
        .register(
            SqlDatabaseConfig.builder()
                .dialect(SqlDatabaseDialect.POSTGRESQL /* TODO: incoming from config server */)
                .address("localhost:9115" /* TODO: incoming from config server */)
                .credentials("postgres", "root" /* TODO: incoming from config server */)
                .databaseName("ns_main" /* TODO: incoming from config server */)
                .build(),
            JdbcSqlClient::new)
        .register(
            SqlDatabaseConfig.builder()
                .dialect(SqlDatabaseDialect.POSTGRESQL /* TODO: incoming from config server */)
                .address("localhost:9115" /* TODO: incoming from config server */)
                .credentials("postgres", "root" /* TODO: incoming from config server */)
                .databaseName("ns_telemetry" /* TODO: incoming from config server */)
                .build(),
            JdbcSqlClient::new)
        .build();
  }
}
