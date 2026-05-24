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
