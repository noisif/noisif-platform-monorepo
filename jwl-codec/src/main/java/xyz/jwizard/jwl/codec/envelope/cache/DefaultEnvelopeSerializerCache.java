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
package xyz.jwizard.jwl.codec.envelope.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializerRegistry;

// for O(1)
public class DefaultEnvelopeSerializerCache implements EnvelopeSerializerCache {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultEnvelopeSerializerCache.class);

    private final Map<String, Map<String, EnvelopeSerializer<?>>> cache = new ConcurrentHashMap<>();

    private DefaultEnvelopeSerializerCache() {
    }

    public static DefaultEnvelopeSerializerCache createDefault() {
        return new DefaultEnvelopeSerializerCache();
    }

    @Override
    public EnvelopeSerializerCache init(EnvelopeSerializerRegistry registry) {
        cache.putAll(registry.getSerializers().stream()
            .collect(Collectors.groupingBy(
                s -> s.baseFormat().getFormat().toLowerCase(),
                Collectors.toMap(
                    s -> s.getCodecDataType().getCode().toLowerCase(),
                    Function.identity()
                )
            )));
        if (LOG.isDebugEnabled()) {
            final int totalSerializers = cache.values().stream().mapToInt(Map::size).sum();
            LOG.debug("EnvelopeSerializerCache initialized: loaded {} encodings with {} total " +
                "frame serializers", cache.size(), totalSerializers);
        }
        return this;
    }

    @Override
    public EnvelopeSerializer<?> find(String encoding, String frame) {
        LOG.trace("Searching for serializer: encoding={}, frame={}", encoding, frame);
        if (encoding == null || frame == null) {
            LOG.warn("Lookup failed: encoding or frame is null (encoding={}, frame={})", encoding,
                frame);
            return null;
        }
        final Map<String, EnvelopeSerializer<?>> frames = cache.get(encoding.toLowerCase());
        if (frames == null) {
            LOG.debug("No serializers found for encoding: '{}'", encoding);
            return null;
        }
        final EnvelopeSerializer<?> serializer = frames.get(frame.toLowerCase());
        if (serializer != null) {
            LOG.debug("Successfully resolved serializer: {} for [{} / {}]",
                serializer.getClass().getSimpleName(), encoding, frame);
            return serializer;
        }
        LOG.warn("No frame serializer '{}' found for encoding '{}', available frames: {}", frame,
            encoding, frames.keySet());
        return null;
    }
}
