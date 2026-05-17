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
package xyz.jwizard.jwl.graph.neo4j.client.factory;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.graph.client.factory.GraphConfig;

public class Neo4jConfig extends GraphConfig {
    private final String username;
    private final String password;

    protected Neo4jConfig(Builder builder) {
        super(builder);
        this.username = builder.username;
        this.password = builder.password;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public static final class Builder extends AbstractBuilder<Builder, Neo4jConfig> {
        private String username;
        private String password;

        private Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        @Override
        protected void validate() {
            super.validate();
            Assert.notNull(username, "Username cannot be null");
            Assert.notNull(password, "Password cannot be null");
        }

        @Override
        public Neo4jConfig build() {
            validate();
            return new Neo4jConfig(this);
        }
    }
}
