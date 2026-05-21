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
package xyz.jwizard.jwl.netclient.rest;

import xyz.jwizard.jwl.codec.serialization.MessageSerializer;
import xyz.jwizard.jwl.codec.serialization.SerializerRegistry;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.netclient.NetworkClient;
import xyz.jwizard.jwl.netclient.rest.group.RestClientGroupConfig;

public abstract class GenericRestClient extends NetworkClient<RestClientGroupConfig>
    implements RestClient {
    protected final boolean followRedirects;
    protected final int maxRedirects;
    protected final SerializerRegistry<MessageSerializer> serializerRegistry;
    protected final ClassScanner scanner;

    protected GenericRestClient(AbstractBuilder<?> builder) {
        super(builder);
        followRedirects = builder.followRedirects;
        maxRedirects = builder.maxRedirects;
        serializerRegistry = builder.serializerRegistry;
        scanner = builder.scanner;
    }

    protected abstract static class AbstractBuilder<B extends AbstractBuilder<B>>
        extends AbstractBaseBuilder<RestClientGroupConfig, B> {
        private boolean followRedirects = true;
        private int maxRedirects = 8;
        private SerializerRegistry<MessageSerializer> serializerRegistry;
        private ClassScanner scanner;

        protected AbstractBuilder() {
            super();
        }

        public B followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return self();
        }

        public B maxRedirects(int maxRedirects) {
            this.maxRedirects = maxRedirects;
            return self();
        }

        public B serializerRegistry(SerializerRegistry<MessageSerializer> serializerRegistry) {
            this.serializerRegistry = serializerRegistry;
            return self();
        }

        public B scanner(ClassScanner scanner) {
            this.scanner = scanner;
            return self();
        }

        @Override
        protected void validate() {
            super.validate();
            Assert.state(maxRedirects > 0, "MaxRedirects must be greater than zero");
            Assert.notNull(serializerRegistry, "SerializerRegistry cannot be null");
            Assert.notNull(scanner, "ClassScanner cannot be null");
        }

        public abstract GenericRestClient build();
    }
}
