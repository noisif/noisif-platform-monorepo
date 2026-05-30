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
package xyz.jwizard.jwl.codec.envelope.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializerRegistry;
import xyz.jwizard.jwl.common.util.StringUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

// for O(1)
public class DefaultEnvelopeSerializerCache implements EnvelopeSerializerCache {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultEnvelopeSerializerCache.class);

  private final Map<String, Map<String, EnvelopeSerializer<?>>> cache = new ConcurrentHashMap<>();

  private DefaultEnvelopeSerializerCache() {}

  public static DefaultEnvelopeSerializerCache createDefault() {
    return new DefaultEnvelopeSerializerCache();
  }

  @Override
  public EnvelopeSerializerCache init(EnvelopeSerializerRegistry registry) {
    cache.putAll(
        registry.getAll().stream()
            .collect(
                Collectors.groupingBy(
                    s -> StringUtil.toLowerCase(s.getBaseFormat().getFormatName()),
                    Collectors.toMap(
                        s -> StringUtil.toLowerCase(s.getCodecDataType().getCode()),
                        Function.identity()))));
    if (LOG.isDebugEnabled()) {
      final int totalSerializers = cache.values().stream().mapToInt(Map::size).sum();
      LOG.debug(
          "EnvelopeSerializerCache initialized: loaded {} encodings with {} total "
              + "frame serializers",
          cache.size(),
          totalSerializers);
    }
    return this;
  }

  @Override
  public EnvelopeSerializer<?> find(String encoding, String frame) {
    LOG.trace("Searching for serializer: encoding={}, frame={}", encoding, frame);
    if (encoding == null || frame == null) {
      LOG.warn("Lookup failed: encoding or frame is null (encoding={}, frame={})", encoding, frame);
      return null;
    }
    final Map<String, EnvelopeSerializer<?>> frames = cache.get(StringUtil.toLowerCase(encoding));
    if (frames == null) {
      LOG.debug("No serializers found for encoding: '{}'", encoding);
      return null;
    }
    final EnvelopeSerializer<?> serializer = frames.get(StringUtil.toLowerCase(frame));
    if (serializer != null) {
      LOG.debug(
          "Successfully resolved serializer: {} for [{} / {}]",
          serializer.getClass().getSimpleName(),
          encoding,
          frame);
      return serializer;
    }
    LOG.warn(
        "No frame serializer '{}' found for encoding '{}', available frames: {}",
        frame,
        encoding,
        frames.keySet());
    return null;
  }
}
