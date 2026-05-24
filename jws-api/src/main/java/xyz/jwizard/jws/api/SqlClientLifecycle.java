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
package xyz.jwizard.jws.api;

import java.util.List;

import xyz.jwizard.jwl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.sql.config.SqlDatabaseConfig;
import xyz.jwizard.jwl.sql.config.SqlDatabaseDialect;
import xyz.jwizard.jwl.sql.jdbc.JdbcSqlClient;
import xyz.jwizard.jwl.sql.pool.hikaricp.HikariConnectionPoolFactory;
import xyz.jwizard.jwl.sql.registry.SqlDatabaseRegistry;

import jakarta.inject.Singleton;

@Singleton
class SqlClientLifecycle implements LifecycleHook {
    private final SqlDatabaseRegistry sqlDatabaseRegistry;

    SqlClientLifecycle() {
        sqlDatabaseRegistry = SqlDatabaseRegistry.builder()
            .poolFactory(HikariConnectionPoolFactory.create())
            .register(SqlDatabaseConfig.builder()
                .dialect(SqlDatabaseDialect.POSTGRESQL /*TODO: incoming from config server*/)
                .address("localhost:9115" /*TODO: incoming from config server*/)
                .credentials("postgres", "root" /*TODO: incoming from config server*/)
                .databaseName("jw_main" /*TODO: incoming from config server*/)
                .build(), JdbcSqlClient::new)
            .register(SqlDatabaseConfig.builder()
                .dialect(SqlDatabaseDialect.POSTGRESQL /*TODO: incoming from config server*/)
                .address("localhost:9115" /*TODO: incoming from config server*/)
                .credentials("postgres", "root" /*TODO: incoming from config server*/)
                .databaseName("jw_telemetry" /*TODO: incoming from config server*/)
                .build(), JdbcSqlClient::new)
            .build();
    }

    @Override
    public List<Class<? extends LifecycleHook>> dependsOn() {
        return List.of(KvServerLifecycle.class);
    }

    @Override
    public void onStart(ComponentProvider componentProvider, ClassScanner scanner) {
        sqlDatabaseRegistry.startAll();
    }

    @Override
    public void onStop() {
        sqlDatabaseRegistry.closeAll();
    }
}
