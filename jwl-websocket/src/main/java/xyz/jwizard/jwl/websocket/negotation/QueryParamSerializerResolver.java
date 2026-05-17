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
package xyz.jwizard.jwl.websocket.negotation;

import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializerRegistry;
import xyz.jwizard.jwl.codec.envelope.cache.DefaultEnvelopeSerializerCache;
import xyz.jwizard.jwl.codec.envelope.cache.EnvelopeSerializerCache;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.common.util.CollectionUtil;
import xyz.jwizard.jwl.websocket.WsHandshakeRequest;

public class QueryParamSerializerResolver implements WsSerializerResolver {
    private final EnvelopeSerializerCache cache;
    private final String encodingParamName;
    private final String frameParamName;

    private QueryParamSerializerResolver(Builder builder) {
        cache = builder.cache.init(builder.registry);
        encodingParamName = builder.encodingParamName;
        frameParamName = builder.frameParamName;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public EnvelopeSerializer<?> resolve(WsHandshakeRequest req) {
        final String encoding = CollectionUtil
            .getFirstSafety(req.getQueryParameter(encodingParamName));
        final String frame = CollectionUtil.getFirstSafety(req.getQueryParameter(frameParamName));
        return cache.find(encoding, frame);
    }

    public static class Builder {
        private EnvelopeSerializerRegistry registry;
        private EnvelopeSerializerCache cache = DefaultEnvelopeSerializerCache.createDefault();
        private String encodingParamName = "encoding";
        private String frameParamName = "frame";

        private Builder() {
        }

        public Builder registry(EnvelopeSerializerRegistry registry) {
            this.registry = registry;
            return this;
        }

        public Builder cache(EnvelopeSerializerCache cache) {
            this.cache = cache;
            return this;
        }

        public Builder encodingParamName(String encodingParamName) {
            this.encodingParamName = encodingParamName;
            return this;
        }

        public Builder frameParamName(String frameParamName) {
            this.frameParamName = frameParamName;
            return this;
        }

        public WsSerializerResolver build() {
            Assert.notNull(registry, "EnvelopeSerializerRegistry cannot be null");
            Assert.notNull(cache, "EnvelopeSerializerCache cannot be null");
            Assert.notNull(encodingParamName, "EncodingParamName cannot be null");
            Assert.notNull(frameParamName, "FrameParamName cannot be null");
            return new QueryParamSerializerResolver(this);
        }
    }
}
