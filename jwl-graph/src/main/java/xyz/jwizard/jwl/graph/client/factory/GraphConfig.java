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
package xyz.jwizard.jwl.graph.client.factory;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.graph.GraphProtocol;
import xyz.jwizard.jwl.net.HostPort;

public abstract class GraphConfig {
    protected final GraphProtocol protocol;
    protected final HostPort address;

    protected GraphConfig(AbstractBuilder<?, ?> builder) {
        this.protocol = builder.protocol;
        this.address = builder.address;
    }

    public GraphProtocol getProtocol() {
        return protocol;
    }

    public HostPort getAddress() {
        return address;
    }

    protected static abstract class AbstractBuilder<B extends AbstractBuilder<B, C>,
        C extends GraphConfig> {
        protected GraphProtocol protocol;
        protected HostPort address;

        protected abstract B self();

        public B protocol(GraphProtocol protocol) {
            this.protocol = protocol;
            return self();
        }

        public B address(HostPort address) {
            this.address = address;
            return self();
        }

        protected void validate() {
            Assert.notNull(protocol, "Protocol cannot be null");
            Assert.notNull(address, "Address cannot be null");
        }

        public abstract C build();
    }
}
