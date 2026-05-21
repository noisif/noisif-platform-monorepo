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

import java.util.function.Consumer;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import xyz.jwizard.jwl.codec.serialization.TypedMessageSerializer;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.net.message.bus.TypedMessageBusListener;
import xyz.jwizard.jwl.netclient.websocket.WsClientSession;
import xyz.jwizard.jwl.netclient.websocket.WsClientUpgradeRequest;
import xyz.jwizard.jwl.netclient.websocket.group.WsClientGroupConfig;
import xyz.jwizard.jwl.netclient.websocket.group.codec.WsSessionCodec;
import xyz.jwizard.jwl.netclient.websocket.group.codec.WsTypedMessageSessionCodec;

public class WsTypedMessageBusConfig extends GenericWsBusConfig {
    private final TypedMessageSerializer<?> typedMessageSerializer;

    private WsTypedMessageBusConfig(Builder builder) {
        super(builder);
        typedMessageSerializer = builder.typedMessageSerializer;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public WsSessionCodec configureProtocol(WsClientUpgradeRequest req) {
        if (encodingParamName != null) {
            req.addQueryParameter(encodingParamName, typedMessageSerializer.getFormat().getFormatName());
        }
        if (dataTypeParamName != null) {
            req.addQueryParameter(dataTypeParamName, typedMessageSerializer.getCodecDataType()
                .getCode());
        }
        return new WsTypedMessageSessionCodec(typedMessageSerializer);
    }

    public static class Builder extends AbstractBuilder<Builder, WsTypedMessageBusConfig> {
        private TypedMessageSerializer<?> typedMessageSerializer;

        private Builder() {
            super();
        }

        @Override
        protected Builder self() {
            return this;
        }

        @CanIgnoreReturnValue
        public Builder serializer(TypedMessageSerializer<?> typedMessageSerializer) {
            this.typedMessageSerializer = typedMessageSerializer;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder addBusListener(TypedMessageBusListener<?, WsClientSession> busListener) {
            return super.addRawBusListener(busListener);
        }

        @Override
        public WsTypedMessageBusConfig build() {
            super.validate();
            Assert.notNull(typedMessageSerializer, "TypedMessageSerializer cannot be null");
            return new WsTypedMessageBusConfig(this);
        }
    }

    public static class Step extends AbstractStep<Step, Builder> {
        public Step(WsClientGroupConfig.Builder parent) {
            super(parent);
        }

        @Override
        protected Step self() {
            return this;
        }

        public Step typedMessageBusConfig(Consumer<Builder> configurer) {
            return super.configure(WsTypedMessageBusConfig.builder(), configurer);
        }
    }
}
