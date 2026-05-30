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

import xyz.jwizard.jwl.sql.GenericSqlClient;
import xyz.jwizard.jwl.sql.SqlDatabaseException;
import xyz.jwizard.jwl.sql.SqlRowMapper;
import xyz.jwizard.jwl.sql.config.SqlDatabaseConfig;
import xyz.jwizard.jwl.sql.pool.ConnectionPoolFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JdbcSqlClient extends GenericSqlClient {
  public JdbcSqlClient(SqlDatabaseConfig config, ConnectionPoolFactory poolFactory) {
    super(config, poolFactory);
  }

  @Override
  public <T> List<T> query(String sql, SqlRowMapper<T> mapper, Object... params) {
    if (log.isDebugEnabled()) {
      log.debug("Executing SQL query: [{}] | Params: {}", sql, Arrays.toString(params));
    }
    try (final Connection conn = getActiveDataSource().getConnection();
        final PreparedStatement stmt = prepareStatement(conn, sql, params);
        final ResultSet rs = stmt.executeQuery()) {

      final List<T> results = new ArrayList<>();
      while (rs.next()) {
        results.add(mapper.map(rs));
      }
      log.debug("Query returned {} row(s)", results.size());
      return results;
    } catch (SQLException e) {
      log.error("Failed to execute query: {}", sql, e);
      throw new SqlDatabaseException("SQL database query failed", e);
    }
  }

  @Override
  public <T> Optional<T> queryForObject(String sql, SqlRowMapper<T> mapper, Object... params) {
    final List<T> results = query(sql, mapper, params);
    if (results.isEmpty()) {
      log.debug("QueryForObject returned empty result for SQL: [{}]", sql);
      return Optional.empty();
    }
    if (results.size() > 1) {
      log.warn("Expected 1 result for query, but found {}. SQL: {}", results.size(), sql);
    }
    return Optional.of(results.getFirst());
  }

  @Override
  public int update(String sql, Object... params) {
    if (log.isDebugEnabled()) {
      log.debug("Executing SQL update: [{}] | Params: {}", sql, Arrays.toString(params));
    }
    try (final Connection conn = getActiveDataSource().getConnection();
        final PreparedStatement stmt = prepareStatement(conn, sql, params)) {
      final int affectedRows = stmt.executeUpdate();
      log.debug("Update affected {} row(s)", affectedRows);
      return affectedRows;
    } catch (SQLException e) {
      log.error("Failed to execute update: {}", sql, e);
      throw new SqlDatabaseException("SQL database update failed", e);
    }
  }

  private PreparedStatement prepareStatement(Connection conn, String sql, Object... params)
      throws SQLException {
    final PreparedStatement stmt = conn.prepareStatement(sql);
    for (int i = 0; i < params.length; i++) {
      stmt.setObject(i + 1, params[i]);
    }
    return stmt;
  }
}
