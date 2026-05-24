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
package xyz.jwizard.jwl.graph.neo4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.graph.GraphServer;
import xyz.jwizard.jwl.graph.client.GraphClient;
import xyz.jwizard.jwl.graph.model.GraphEdge;
import xyz.jwizard.jwl.graph.model.GraphNode;
import xyz.jwizard.jwl.graph.neo4j.client.factory.DefaultNeo4jClientFactory;
import xyz.jwizard.jwl.graph.neo4j.client.factory.Neo4jConfig;
import xyz.jwizard.jwl.graph.neo4j.repository.Neo4jGraphRepository;
import xyz.jwizard.jwl.graph.repository.GraphRepository;
import xyz.jwizard.jwl.net.HostPort;

@Testcontainers
class Neo4jIntegrationTest {
    private static final String PASSWORD = "secret123";

    @Container
    static final Neo4jContainer neo4jContainer = new Neo4jContainer(
        DockerImageName.parse("neo4j:5.12.0")
    ).withAdminPassword(PASSWORD);

    private GraphServer<Neo4jConfig> server;
    private GraphRepository repository;
    private GraphClient client;

    @BeforeEach
    void setUp() {
        final Neo4jConfig config = Neo4jConfig.builder()
            .protocol(Neo4jGraphProtocol.BOLT)
            .address(HostPort.from(
                neo4jContainer.getHost(),
                neo4jContainer.getMappedPort(7687)
            ))
            .username("neo4j")
            .password(PASSWORD)
            .build();
        server = Neo4jServer.builder()
            .config(config)
            .clientFactory(DefaultNeo4jClientFactory.create())
            .repositoryFactory(Neo4jGraphRepository::createDefault)
            .build();
        server.start();
        repository = server.getRepository();
        client = server.getClient();
    }

    @AfterEach
    void tearDown() {
        IoUtil.closeQuietly(server, s -> {
            client.execute("MATCH (n) DETACH DELETE n", Map.of());
            s.close();
        });
    }

    @Test
    @DisplayName("should upsert nodes in batch and find them by label and id successfully")
    void shouldUpsertNodesAndFindThem() {
        // given
        final GraphNode user1 = new GraphNode("User", "u1", Map.of("name", "Alice", "age", 30L));
        final GraphNode user2 = new GraphNode("User", "u2", Map.of("name", "Bob", "age", 25L));
        // when
        repository.upsertNodes(List.of(user1, user2));
        final List<GraphNode> users = repository.findAllNodes("User");
        final GraphNode alice = repository.findNodeById("u1");
        // then
        assertThat(users).hasSize(2);
        assertThat(alice).isNotNull();
        assertThat(alice.props()).containsEntry("name", "Alice");
        assertThat(alice.props()).containsEntry("age", 30L);
    }

    @Test
    @DisplayName("should create edges between existing nodes and retrieve all relationships")
    void shouldCreateEdgesBetweenNodes() {
        // given
        repository.upsertNodes(List.of(
            new GraphNode("User", "u1", Map.of("name", "Alice")),
            new GraphNode("User", "u2", Map.of("name", "Bob"))
        ));
        final GraphEdge edge = new GraphEdge("KNOWS", "u1", "u2", Map.of("since", 2023L));
        // when
        repository.createEdges(List.of(edge));
        final List<GraphEdge> edges = repository.findAllEdges();
        // then
        assertThat(edges).hasSize(1);
        final GraphEdge savedEdge = edges.getFirst();
        assertThat(savedEdge.type()).isEqualTo("KNOWS");
        assertThat(savedEdge.fromId()).isEqualTo("u1");
        assertThat(savedEdge.toId()).isEqualTo("u2");
        assertThat(savedEdge.props()).containsEntry("since", 2023L);
    }

    @Test
    @DisplayName("should save node and edge using repository abstraction and return persisted data")
    void shouldSaveAndReturnDataViaRepository() {
        // given
        final GraphNode nodeA = new GraphNode("Server", "srv-1", Map.of(
            "ip", "10.0.0.1", "status", "ACTIVE"
        ));
        final GraphNode nodeB = new GraphNode("Server", "srv-2", Map.of(
            "ip", "10.0.0.2", "status", "MAINTENANCE"
        ));
        // when
        final GraphNode savedNodeA = repository.saveNode(nodeA);
        repository.saveNode(nodeB);
        // then
        assertThat(savedNodeA).as("Saved node should be returned").isNotNull();
        assertThat(savedNodeA.id()).isEqualTo("srv-1");
        assertThat(savedNodeA.label()).isEqualTo("Server");
        assertThat(savedNodeA.props()).containsEntry("status", "ACTIVE");
        // when
        final GraphEdge connection = new GraphEdge("CONNECTED_TO", "srv-1", "srv-2", Map.of(
            "ping", 12L
        ));
        final GraphEdge savedEdge = repository.saveEdge(connection);
        // then
        assertThat(savedEdge).as("Saved edge should be returned").isNotNull();
        assertThat(savedEdge.type()).isEqualTo("CONNECTED_TO");
        assertThat(savedEdge.fromId()).isEqualTo("srv-1");
        assertThat(savedEdge.toId()).isEqualTo("srv-2");
        assertThat(savedEdge.props()).containsEntry("ping", 12L);
    }

    @Test
    @DisplayName("should execute raw cypher queries directly via GraphClient")
    void shouldExecuteRawCypherQueriesDirectlyViaGraphClient() {
        // when
        client.execute(
            "CREATE (d:Device {deviceId: $id, status: $status})",
            Map.of("id", "dev-99", "status", "ONLINE")
        );
        final List<Map<String, Object>> results = client.read(
            """
                MATCH (d:Device) WHERE d.status = $status
                RETURN d.deviceId AS id, d.status AS status
                """,
            Map.of("status", "ONLINE")
        );
        // then
        assertThat(results).hasSize(1);
        final Map<String, Object> row = results.getFirst();
        assertThat(row).containsEntry("id", "dev-99");
        assertThat(row).containsEntry("status", "ONLINE");
    }
}
