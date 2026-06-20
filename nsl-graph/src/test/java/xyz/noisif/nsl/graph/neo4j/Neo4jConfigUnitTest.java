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
package xyz.noisif.nsl.graph.neo4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.noisif.nsl.common.bootstrap.CriticalBootstrapException;
import xyz.noisif.nsl.graph.neo4j.client.factory.Neo4jConfig;
import xyz.noisif.nsl.net.HostPort;

class Neo4jConfigUnitTest {
  @Test
  @DisplayName("should build valid Neo4jConfig when all required properties are provided")
  void shouldBuildValidConfig() {
    // when
    final Neo4jConfig config =
        Neo4jConfig.builder()
            .protocol(Neo4jGraphProtocol.NEO4J_S)
            .address(HostPort.from("localhost", 7687))
            .username("admin")
            .password("pass")
            .build();
    // then
    assertThat(config.getProtocol()).isEqualTo(Neo4jGraphProtocol.NEO4J_S);
    assertThat(config.getAddress().host()).isEqualTo("localhost");
    assertThat(config.getUsername()).isEqualTo("admin");
  }

  @Test
  @DisplayName("should throw CriticalBootstrapException when password is not provided")
  void shouldThrowExceptionWhenPasswordIsMissing() {
    // given
    final Neo4jConfig.Builder builder =
        Neo4jConfig.builder()
            .protocol(Neo4jGraphProtocol.BOLT)
            .address(HostPort.from("localhost", 7687))
            .username("admin");
    // when & then
    assertThatThrownBy(builder::build)
        .isInstanceOf(CriticalBootstrapException.class)
        .hasMessageContaining("object 'password' cannot be null");
  }

  @Test
  @DisplayName("should throw CriticalBootstrapException when base protocol is missing")
  void shouldThrowExceptionWhenProtocolIsMissing() {
    // given
    final Neo4jConfig.Builder builder =
        Neo4jConfig.builder()
            .address(HostPort.from("localhost", 7687))
            .username("admin")
            .password("pass");
    // when & then
    assertThatThrownBy(builder::build)
        .isInstanceOf(CriticalBootstrapException.class)
        .hasMessageContaining("object 'protocol' cannot be null");
  }
}
