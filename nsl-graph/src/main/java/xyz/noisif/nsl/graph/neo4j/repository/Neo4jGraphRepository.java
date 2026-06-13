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
package xyz.noisif.nsl.graph.neo4j.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.util.CastUtil;
import xyz.noisif.nsl.graph.client.GraphClient;
import xyz.noisif.nsl.graph.model.GraphEdge;
import xyz.noisif.nsl.graph.model.GraphNode;
import xyz.noisif.nsl.graph.repository.GraphRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Neo4jGraphRepository implements GraphRepository {
  private static final Logger LOG = LoggerFactory.getLogger(Neo4jGraphRepository.class);

  private final GraphClient graphClient;

  private Neo4jGraphRepository(GraphClient graphClient) {
    this.graphClient = graphClient;
  }

  public static Neo4jGraphRepository createDefault(GraphClient graphClient) {
    return new Neo4jGraphRepository(graphClient);
  }

  @Override
  public List<GraphNode> findAllNodes(String label) {
    LOG.debug("Finding all nodes with label: {}", label);
    final String cypher =
        String.format(
            """
          MATCH (n:%s) RETURN n.id AS id, properties(n) AS props
          """,
            label);
    final List<GraphNode> nodes =
        graphClient.read(cypher, Map.of()).stream()
            .map(
                row ->
                    new GraphNode(
                        label, (String) row.get("id"), CastUtil.unsafeCast(row.get("props"))))
            .toList();
    LOG.debug("Found {} nodes for label: {}", nodes.size(), label);
    return nodes;
  }

  @Override
  public List<GraphEdge> findAllEdges() {
    LOG.debug("Finding all edges in the graph");
    final String cypher =
        """
        MATCH (n)-[r]->(m)
        RETURN type(r) AS type, n.id AS from, m.id AS to, properties(r) AS props
        """;
    final List<GraphEdge> edges =
        graphClient.read(cypher, Map.of()).stream()
            .map(
                row ->
                    new GraphEdge(
                        (String) row.get("type"),
                        (String) row.get("from"),
                        (String) row.get("to"),
                        CastUtil.unsafeCast(row.get("props"))))
            .toList();
    LOG.debug("Found {} edges total", edges.size());
    return edges;
  }

  @Override
  public GraphNode findNodeById(String id) {
    LOG.debug("Finding node by id: {}", id);
    final String cypher =
        """
        MATCH (n {id: $id})
        RETURN labels(n)[0] AS label, n.id AS id, properties(n) AS props
        """;
    final List<Map<String, Object>> res = graphClient.read(cypher, Map.of("id", id));
    if (res.isEmpty()) {
      LOG.debug("Node with id: {} not found", id);
      return null;
    }
    return new GraphNode(
        (String) res.getFirst().get("label"), id, CastUtil.unsafeCast(res.getFirst().get("props")));
  }

  @Override
  public GraphNode saveNode(GraphNode node) {
    LOG.debug("Saving node: {} with id: {}", node.label(), node.id());
    final String cypher =
        String.format(
            """
          MERGE (n:%s {id: $id}) SET n += $props
          RETURN labels(n)[0] AS label, n.id AS id, properties(n) AS props
          """,
            node.label());
    final List<Map<String, Object>> res =
        graphClient.write(
            cypher,
            Map.of(
                "id", node.id(),
                "props", node.props()));
    return res.isEmpty()
        ? null
        : new GraphNode(
            (String) res.getFirst().get("label"),
            (String) res.getFirst().get("id"),
            CastUtil.unsafeCast(res.getFirst().get("props")));
  }

  @Override
  public GraphEdge saveEdge(GraphEdge edge) {
    LOG.debug("Saving edge: {} from: {} to: {}", edge.type(), edge.fromId(), edge.toId());
    final String cypher =
        String.format(
            """
          MATCH (a {id: $fromId}), (b {id: $toId})
          MERGE (a)-[r:%s]->(b) SET r += $props
          RETURN type(r) AS type, a.id AS from, b.id AS to, properties(r) AS props
          """,
            edge.type());
    final List<Map<String, Object>> res =
        graphClient.write(
            cypher,
            Map.of(
                "fromId", edge.fromId(),
                "toId", edge.toId(),
                "props", edge.props()));
    return res.isEmpty()
        ? null
        : new GraphEdge(
            (String) res.getFirst().get("type"),
            (String) res.getFirst().get("from"),
            (String) res.getFirst().get("to"),
            CastUtil.unsafeCast(res.getFirst().get("props")));
  }

  @Override
  public void upsertNodes(List<GraphNode> nodes) {
    LOG.debug("Upserting {} nodes", nodes.size());
    final Map<String, List<Map<String, Object>>> grouped =
        nodes.stream()
            .collect(
                Collectors.groupingBy(
                    GraphNode::label,
                    Collectors.mapping(
                        n ->
                            Map.of(
                                "id", n.id(),
                                "props", n.props()),
                        Collectors.toList())));
    for (final Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
      LOG.trace("Batch upserting {} nodes for label: {}", entry.getValue().size(), entry.getKey());
      final String cypher =
          String.format(
              """
            UNWIND $batch AS row MERGE (n:%s {id: row.id}) SET n += row.props
            """,
              entry.getKey());
      graphClient.execute(cypher, Map.of("batch", entry.getValue()));
    }
  }

  @Override
  public void createEdges(List<GraphEdge> edges) {
    LOG.debug("Creating/Updating {} edges", edges.size());
    final Map<String, List<Map<String, Object>>> grouped =
        edges.stream()
            .collect(
                Collectors.groupingBy(
                    GraphEdge::type,
                    Collectors.mapping(
                        e ->
                            Map.of(
                                "fromId", e.fromId(),
                                "toId", e.toId(),
                                "props", e.props()),
                        Collectors.toList())));
    for (final Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
      LOG.trace("Batch creating {} edges of type: {}", entry.getValue().size(), entry.getKey());
      final String cypher =
          String.format(
              """
            UNWIND $batch AS row MATCH (a {id: row.fromId}), (b {id: row.toId})
            MERGE (a)-[r:%s]->(b) SET r += row.props
            """,
              entry.getKey());
      graphClient.execute(cypher, Map.of("batch", entry.getValue()));
    }
  }
}
