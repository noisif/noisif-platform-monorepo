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
package xyz.jwizard.jwl.graph.neo4j.client;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.MapAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.graph.GraphDatabaseException;
import xyz.jwizard.jwl.graph.client.GraphClient;

import java.util.List;
import java.util.Map;

public class Neo4jClient implements GraphClient {
  private static final Logger LOG = LoggerFactory.getLogger(Neo4jClient.class);

  private final Driver driver;

  public Neo4jClient(Driver driver) {
    this.driver = driver;
  }

  @Override
  public List<Map<String, Object>> read(String query, Map<String, Object> parameters) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Executing read query: {} with params: {}", query.replace("\n", " "), parameters);
    }
    try (final Session session = driver.session()) {
      return session.executeRead(tx -> tx.run(query, parameters).list(MapAccessor::asMap));
    } catch (Exception ex) {
      throw new GraphDatabaseException("Neo4j read transaction failed", ex);
    }
  }

  @Override
  public List<Map<String, Object>> write(String query, Map<String, Object> parameters) {
    if (LOG.isTraceEnabled()) {
      LOG.trace(
          "Executing write query with return: {} with params: {}",
          query.replace("\n", " "),
          parameters);
    }
    try (final Session session = driver.session()) {
      return session.executeWrite(tx -> tx.run(query, parameters).list(MapAccessor::asMap));
    } catch (Exception ex) {
      throw new GraphDatabaseException("Neo4j write transaction failed", ex);
    }
  }

  @Override
  public void execute(String query, Map<String, Object> parameters) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Executing write query: {} with params: {}", query.replace("\n", " "), parameters);
    }
    try (final Session session = driver.session()) {
      session.executeWrite(
          tx -> {
            tx.run(query, parameters).consume();
            return null;
          });
    } catch (Exception ex) {
      throw new GraphDatabaseException("Neo4j execution failed", ex);
    }
  }

  @Override
  public void close() {
    IoUtil.closeQuietly(driver, Driver::close);
  }
}
