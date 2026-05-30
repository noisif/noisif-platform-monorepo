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
package xyz.jwizard.jwl.sql.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.sql.SqlClient;
import xyz.jwizard.jwl.sql.config.SqlDatabaseConfig;
import xyz.jwizard.jwl.sql.config.SqlDatabaseDialect;
import xyz.jwizard.jwl.sql.pool.hikaricp.HikariConnectionPoolFactory;
import xyz.jwizard.jwl.sql.registry.SqlDatabaseRegistry;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Testcontainers
class JdbcSqlClientIntegrationTest {
  @Container
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
          .withDatabaseName("jwizard_test")
          .withUsername("testuser")
          .withPassword("testpass");

  private static SqlDatabaseRegistry registry;
  private static SqlClient db;

  @BeforeAll
  static void setUp() {
    final String address = postgres.getHost() + ":" + postgres.getFirstMappedPort();
    final SqlDatabaseConfig config =
        SqlDatabaseConfig.builder()
            .dialect(SqlDatabaseDialect.POSTGRESQL)
            .address(address)
            .credentials(postgres.getUsername(), postgres.getPassword())
            .databaseName(postgres.getDatabaseName())
            .build();

    registry =
        SqlDatabaseRegistry.builder()
            .poolFactory(HikariConnectionPoolFactory.create())
            .register(config, JdbcSqlClient::new)
            .build();

    registry.startAll();
    db = registry.getClient(postgres.getDatabaseName());

    db.update(
        """
                CREATE TABLE users (
                    id SERIAL PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    age INT NOT NULL
                )
            """);
    db.update(
        """
                CREATE TABLE documents (
                    id UUID PRIMARY KEY,
                    payload JSONB NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
            """);
  }

  @AfterAll
  static void tearDown() {
    IoUtil.closeQuietly(registry, SqlDatabaseRegistry::closeAll);
  }

  @Test
  @DisplayName("should insert and query multiple rows successfully")
  void shouldInsertAndQueryMultipleRows() {
    // given
    db.update("INSERT INTO users (username, age) VALUES (?, ?)", "Alice", 25);
    db.update("INSERT INTO users (username, age) VALUES (?, ?)", "Bob", 30);
    // when
    final List<User> users =
        db.query(
            "SELECT * FROM users ORDER BY id ASC",
            rs -> new User(rs.getInt("id"), rs.getString("username"), rs.getInt("age")));
    // then
    assertEquals(2, users.size());
    assertEquals("Alice", users.get(0).username());
    assertEquals("Bob", users.get(1).username());
  }

  @Test
  @DisplayName("should return single mapped object correctly")
  void shouldReturnSingleObjectCorrectly() {
    // given
    db.update("INSERT INTO users (username, age) VALUES (?, ?)", "Charlie", 40);
    // when
    final Optional<User> user =
        db.queryForObject(
            "SELECT * FROM users WHERE username = ?",
            rs -> new User(rs.getInt("id"), rs.getString("username"), rs.getInt("age")),
            "Charlie");
    // then
    assertTrue(user.isPresent());
    assertEquals("Charlie", user.get().username());
    assertEquals(40, user.get().age());
  }

  @Test
  @DisplayName("should return empty Optional when no row is found")
  void shouldReturnEmptyOptionalWhenNoRowFound() {
    // when
    final Optional<User> user =
        db.queryForObject(
            "SELECT * FROM users WHERE username = ?",
            rs -> new User(rs.getInt("id"), rs.getString("username"), rs.getInt("age")),
            "Ghost");
    // then
    assertTrue(user.isEmpty());
  }

  @Test
  @DisplayName("should update and delete rows correctly")
  void shouldUpdateAndDeleteRows() {
    // given
    db.update("INSERT INTO users (username, age) VALUES (?, ?)", "David", 45);
    // when: UPDATE
    final int updatedRows = db.update("UPDATE users SET age = ? WHERE username = ?", 46, "David");
    // then: UPDATE
    assertEquals(1, updatedRows);
    final Optional<User> updatedUser =
        db.queryForObject(
            "SELECT * FROM users WHERE username = ?",
            rs -> new User(rs.getInt("id"), rs.getString("username"), rs.getInt("age")),
            "David");
    assertTrue(updatedUser.isPresent());
    assertEquals(46, updatedUser.get().age());
    // when: DELETE
    final int deletedRows = db.update("DELETE FROM users WHERE username = ?", "David");
    // then: DELETE
    assertEquals(1, deletedRows);
    final Optional<User> deletedUser =
        db.queryForObject(
            "SELECT * FROM users WHERE username = ?",
            rs -> new User(rs.getInt("id"), rs.getString("username"), rs.getInt("age")),
            "David");
    assertTrue(deletedUser.isEmpty());
  }

  @Test
  @DisplayName("should handle complex types like UUID, JSONB and LocalDateTime properly")
  void shouldHandleComplexTypesProperly() {
    // given
    final UUID id = UUID.randomUUID();
    final String jsonPayload = "{\"key\": \"value\", \"active\": true}";
    final Instant createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    final int inserted =
        db.update(
            "INSERT INTO documents (id, payload, created_at) VALUES (?, ?::jsonb, ?)",
            id,
            jsonPayload,
            Timestamp.from(createdAt));
    assertEquals(1, inserted);
    // when
    final Optional<Document> doc =
        db.queryForObject(
            "SELECT * FROM documents WHERE id = ?",
            rs ->
                new Document(
                    rs.getObject("id", UUID.class),
                    rs.getString("payload"),
                    rs.getTimestamp("created_at").toInstant()),
            id);
    // then
    assertTrue(doc.isPresent());
    assertEquals(id, doc.get().id());
    assertEquals(jsonPayload, doc.get().payload());
    assertEquals(createdAt, doc.get().createdAt());
  }
}

record User(int id, String username, int age) {}

record Document(UUID id, String payload, Instant createdAt) {}
