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
package xyz.noisif.nss.ingestor.graph;

import xyz.noisif.nsl.graph.GraphReader;
import xyz.noisif.nsl.graph.GraphServer;
import xyz.noisif.nsl.graph.GraphWriter;
import xyz.noisif.nsl.graph.client.GraphClient;
import xyz.noisif.nsl.graph.neo4j.Neo4jGraphProtocol;
import xyz.noisif.nsl.graph.neo4j.Neo4jServer;
import xyz.noisif.nsl.graph.neo4j.client.factory.DefaultNeo4jClientFactory;
import xyz.noisif.nsl.graph.neo4j.client.factory.Neo4jConfig;
import xyz.noisif.nsl.graph.neo4j.repository.Neo4jGraphRepository;
import xyz.noisif.nsl.net.HostPort;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
class GraphServerConfiguration {
  @Produces
  @Singleton
  GraphServer<Neo4jConfig> graphServer() {
    return Neo4jServer.builder()
        .config(
            Neo4jConfig.builder()
                .protocol(Neo4jGraphProtocol.BOLT) /* TODO: incoming from config server */
                .address(HostPort.from("localhost", 9118)) /* TODO: incoming from config server */
                .username("neo4j") /* TODO: incoming from config server */
                .password("root") /* TODO: incoming from config server */
                .build())
        .clientFactory(DefaultNeo4jClientFactory.create())
        .repositoryFactory(Neo4jGraphRepository::createDefault)
        .build();
  }

  @Produces
  @Singleton
  GraphReader graphReader(GraphServer<Neo4jConfig> graphServer) {
    return graphServer.getRepository();
  }

  @Produces
  @Singleton
  GraphWriter graphWriter(GraphServer<Neo4jConfig> graphServer) {
    return graphServer.getRepository();
  }

  @Produces
  @Singleton
  GraphClient graphClient(GraphServer<Neo4jConfig> graphServer) {
    return graphServer.getClient();
  }
}
