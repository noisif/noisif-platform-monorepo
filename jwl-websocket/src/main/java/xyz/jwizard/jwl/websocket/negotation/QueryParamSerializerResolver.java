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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializerRegistry;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.websocket.WsHandshakeRequest;

// with O(1) cache
public class QueryParamSerializerResolver implements WsSerializerResolver {
    private final Map<String, Map<String, EnvelopeSerializer<?>>> formatCache;
    private final String encodingParamName;
    private final String frameParamName;

    private QueryParamSerializerResolver(Builder builder) {
        formatCache = builder.registry.getSerializers().stream()
            .collect(Collectors.groupingBy(
                s -> s.baseFormat().getFormat().toLowerCase(),
                Collectors.toMap(
                    s -> s.getCodecDataType().getCode().toLowerCase(),
                    Function.identity()
                )
            ));
        encodingParamName = builder.encodingParamName;
        frameParamName = builder.frameParamName;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public EnvelopeSerializer<?> resolve(WsHandshakeRequest req) {
        final String encoding = getFirst(req.getQueryParameter(encodingParamName));
        if (encoding == null) {
            return null;
        }
        final Map<String, EnvelopeSerializer<?>> frame = formatCache.get(encoding.toLowerCase());
        if (frame == null) {
            return null;
        }
        final String frm = getFirst(req.getQueryParameter(frameParamName));
        if (frm == null) {
            return null;
        }
        return frame.get(frm.toLowerCase());
    }

    private String getFirst(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        final String first = values.getFirst();
        return (first == null || first.isBlank()) ? null : first;
    }

    public static class Builder {
        private EnvelopeSerializerRegistry registry;
        private String encodingParamName = "encoding";
        private String frameParamName = "frame";

        private Builder() {
        }

        public Builder registry(EnvelopeSerializerRegistry registry) {
            this.registry = registry;
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
            Assert.notNull(encodingParamName, "EncodingParamName cannot be null");
            Assert.notNull(frameParamName, "FrameParamName cannot be null");
            return new QueryParamSerializerResolver(this);
        }
    }
}
