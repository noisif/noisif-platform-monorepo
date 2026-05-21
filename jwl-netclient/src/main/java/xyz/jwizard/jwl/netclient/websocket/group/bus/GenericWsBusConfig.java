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
package xyz.jwizard.jwl.netclient.websocket.group.bus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.net.bus.CompositeBusListener;
import xyz.jwizard.jwl.net.bus.RawBusListener;
import xyz.jwizard.jwl.netclient.websocket.WsClientSession;
import xyz.jwizard.jwl.netclient.websocket.group.WsClientGroupConfig;

abstract class GenericWsBusConfig implements WsBusConfig {
    protected final String encodingParamName;
    protected final String dataTypeParamName;
    protected final RawBusListener<WsClientSession> busListener;

    protected GenericWsBusConfig(AbstractBuilder<?, ?> builder) {
        this.encodingParamName = builder.encodingParamName;
        this.dataTypeParamName = builder.dataTypeParamName;
        this.busListener = CompositeBusListener.load(builder.busListeners);
    }

    @Override
    public RawBusListener<WsClientSession> getBusListener() {
        return busListener;
    }

    abstract static class AbstractBuilder<B extends AbstractBuilder<B, C>,
        C extends GenericWsBusConfig> {
        protected final List<RawBusListener<WsClientSession>> busListeners = new ArrayList<>();
        private final boolean paramsRequired;
        protected String encodingParamName;
        protected String dataTypeParamName;

        protected AbstractBuilder(String encodingParamName, String dataTypeParamName,
                                  boolean paramsRequired) {
            this.encodingParamName = encodingParamName;
            this.dataTypeParamName = dataTypeParamName;
            this.paramsRequired = paramsRequired;
        }

        protected AbstractBuilder(String encodingParamName, String dataTypeParamName) {
            this(encodingParamName, dataTypeParamName, true);
        }

        protected AbstractBuilder() {
            this(null, null, false);
        }

        protected abstract B self();

        public B encodingParamName(String encodingParamName) {
            this.encodingParamName = encodingParamName;
            return self();
        }

        public B dataTypeParamName(String dataTypeParamName) {
            this.dataTypeParamName = dataTypeParamName;
            return self();
        }

        protected B addRawBusListener(RawBusListener<WsClientSession> listener) {
            busListeners.add(listener);
            return self();
        }

        protected void validate() {
            if (paramsRequired) {
                Assert.notNull(encodingParamName, "EncodingParamName cannot be null");
                Assert.notNull(dataTypeParamName, "DataTypeParamName cannot be null");
            }
            Assert.notNullAll(busListeners, "All BusListeners must be initialized");
        }

        public abstract C build();
    }

    abstract static class AbstractStep<S extends AbstractStep<S, B>,
        B extends AbstractBuilder<B, ?>> {
        protected final WsClientGroupConfig.Builder parent;
        protected WsBusConfig busConfig;

        protected AbstractStep(WsClientGroupConfig.Builder parent) {
            this.parent = parent;
        }

        protected abstract S self();

        protected S configure(B builder, Consumer<B> configurer) {
            configurer.accept(builder);
            this.busConfig = builder.build();
            return self();
        }

        public WsClientGroupConfig build() {
            return new WsClientGroupConfig(parent, busConfig);
        }
    }
}
