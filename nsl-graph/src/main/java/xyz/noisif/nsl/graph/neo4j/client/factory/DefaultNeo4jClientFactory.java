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
package xyz.noisif.nsl.graph.neo4j.client.factory;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.graph.client.GraphClient;
import xyz.noisif.nsl.graph.client.factory.GraphClientFactory;
import xyz.noisif.nsl.graph.neo4j.client.Neo4jClient;
import xyz.noisif.nsl.net.NetworkUtil;

import java.net.URI;

public class DefaultNeo4jClientFactory implements GraphClientFactory<Neo4jConfig> {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultNeo4jClientFactory.class);

  private DefaultNeo4jClientFactory() {}

  public static DefaultNeo4jClientFactory create() {
    return new DefaultNeo4jClientFactory();
  }

  @Override
  public GraphClient createAndInitClient(Neo4jConfig config) {
    final URI uri = NetworkUtil.parseToUri(config.getProtocol(), config.getAddress());
    LOG.debug("Initializing Neo4j driver for: {}", uri);

    final Config.ConfigBuilder configBuilder = Config.builder();
    if (config.getProtocol().isEncrypted()) {
      LOG.trace(
          "Neo4j encryption enabled (strict: {})",
          config.getProtocol().requestStrictTlsValidation());
      configBuilder.withEncryption();
      if (config.getProtocol().requestStrictTlsValidation()) {
        configBuilder.withTrustStrategy(Config.TrustStrategy.trustSystemCertificates());
      } else {
        configBuilder.withTrustStrategy(Config.TrustStrategy.trustAllCertificates());
      }
    } else {
      LOG.trace("Neo4j encryption disabled");
      configBuilder.withoutEncryption();
    }
    final Driver driver =
        GraphDatabase.driver(
            NetworkUtil.parseToUri(config.getProtocol(), config.getAddress()),
            AuthTokens.basic(config.getUsername(), config.getPassword()),
            configBuilder.build());
    LOG.debug("Verifying connectivity to Neo4j");
    try {
      driver.verifyConnectivity();
      LOG.info("Successfully connected to Neo4j at {}", uri);
    } catch (Exception ex) {
      driver.close();
      throw ex;
    }
    return new Neo4jClient(driver);
  }
}
