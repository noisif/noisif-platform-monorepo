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
package xyz.jwizard.jws.ingestor;

import java.util.List;

import xyz.jwizard.jwl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.graph.GraphReader;
import xyz.jwizard.jwl.graph.GraphServer;
import xyz.jwizard.jwl.graph.GraphWriter;
import xyz.jwizard.jwl.graph.client.GraphClient;
import xyz.jwizard.jwl.graph.neo4j.Neo4jGraphProtocol;
import xyz.jwizard.jwl.graph.neo4j.Neo4jServer;
import xyz.jwizard.jwl.graph.neo4j.client.factory.DefaultNeo4jClientFactory;
import xyz.jwizard.jwl.graph.neo4j.client.factory.Neo4jConfig;
import xyz.jwizard.jwl.graph.neo4j.repository.Neo4jGraphRepository;
import xyz.jwizard.jwl.net.HostPort;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
class GraphServerLifecycle implements LifecycleHook {
    private final GraphServer<Neo4jConfig> graphServer;

    GraphServerLifecycle() {
        graphServer = Neo4jServer.builder()
            .config(Neo4jConfig.builder()
                .protocol(Neo4jGraphProtocol.BOLT) /*TODO: incoming from config server*/
                .address(HostPort.from("localhost", 9118)) /*TODO: incoming from config server*/
                .username("neo4j") /*TODO: incoming from config server*/
                .password("root") /*TODO: incoming from config server*/
                .build()
            )
            .clientFactory(DefaultNeo4jClientFactory.create())
            .repositoryFactory(Neo4jGraphRepository::createDefault)
            .build();
    }

    @Override
    public void onStart(ComponentProvider componentProvider, ClassScanner scanner) {
        graphServer.start();
    }

    @Override
    public void onStop() {
        graphServer.close();
    }

    @Override
    public List<Class<? extends LifecycleHook>> dependsOn() {
        return List.of(JsEngineLifecycle.class);
    }

    @Produces
    @Singleton
    GraphReader graphReader() {
        return graphServer.getRepository();
    }

    @Produces
    @Singleton
    GraphWriter graphWriter() {
        return graphServer.getRepository();
    }

    @Produces
    @Singleton
    GraphClient graphClient() {
        return graphServer.getClient();
    }
}
