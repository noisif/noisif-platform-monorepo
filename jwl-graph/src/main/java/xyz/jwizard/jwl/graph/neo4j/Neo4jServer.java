/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jwizard.jwl.graph.neo4j;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.graph.GraphServer;
import xyz.jwizard.jwl.graph.neo4j.client.factory.Neo4jConfig;

public class Neo4jServer extends GraphServer<Neo4jConfig> {
    protected Neo4jServer(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<Builder, Neo4jConfig> {
        private Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected void validate() {
            super.validate();
            Assert.notNull(config.getUsername(), "Username cannot be null");
            Assert.notNull(config.getPassword(), "Password cannot be null");
        }

        @Override
        public GraphServer<Neo4jConfig> build() {
            validate();
            return new Neo4jServer(this);
        }
    }
}
