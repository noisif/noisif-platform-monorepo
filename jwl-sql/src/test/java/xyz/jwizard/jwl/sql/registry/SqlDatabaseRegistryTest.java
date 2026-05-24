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
package xyz.jwizard.jwl.sql.registry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import xyz.jwizard.jwl.sql.SqlClient;
import xyz.jwizard.jwl.sql.config.SqlDatabaseConfig;
import xyz.jwizard.jwl.sql.config.SqlDatabaseDialect;
import xyz.jwizard.jwl.sql.jdbc.JdbcSqlClient;
import xyz.jwizard.jwl.sql.pool.ConnectionPoolFactory;

@ExtendWith(MockitoExtension.class)
class SqlDatabaseRegistryTest {
    @Mock
    private ConnectionPoolFactory mockPoolFactory;

    @Test
    @DisplayName("should register and retrieve SQL client successfully")
    void shouldRegisterAndRetrieveClient() {
        // given
        final SqlDatabaseConfig config = SqlDatabaseConfig.builder()
            .dialect(SqlDatabaseDialect.POSTGRESQL)
            .address("localhost:5432")
            .credentials("user", "pass")
            .databaseName("test_db")
            .build();
        final SqlDatabaseRegistry registry = SqlDatabaseRegistry.builder()
            .poolFactory(mockPoolFactory)
            .register(config, JdbcSqlClient::new)
            .build();
        // when
        final SqlClient client = registry.getClient("test_db");
        // then
        assertNotNull(client);
    }

    @Test
    @DisplayName("should throw exception when registering duplicate database name")
    void shouldThrowExceptionWhenRegisteringDuplicateDatabase() {
        // given
        final SqlDatabaseConfig config = SqlDatabaseConfig.builder()
            .dialect(SqlDatabaseDialect.POSTGRESQL)
            .address("localhost:5432")
            .credentials("user", "pass")
            .databaseName("duplicate_db")
            .build();
        // when & then
        final IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> SqlDatabaseRegistry.builder()
                .poolFactory(mockPoolFactory)
                .register(config, JdbcSqlClient::new)
                .register(config, JdbcSqlClient::new)
                .build());
        assertTrue(exception.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("should throw exception when getting non-existent database client")
    void shouldThrowExceptionWhenGettingNonExistentDatabase() {
        // given
        final SqlDatabaseRegistry registry = SqlDatabaseRegistry.builder()
            .poolFactory(mockPoolFactory)
            .build();
        // when & then
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> registry.getClient("ghost_db"));
        assertTrue(exception.getMessage().contains("No registered element found"));
    }
}
