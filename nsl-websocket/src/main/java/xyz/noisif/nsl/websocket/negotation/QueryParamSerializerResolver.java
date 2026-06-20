/*
 * Copyright (c) 2022-2026 NOISIF. All Rights Reserved.
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
package xyz.noisif.nsl.websocket.negotation;

import xyz.noisif.nsl.codec.envelope.EnvelopeSerializer;
import xyz.noisif.nsl.codec.envelope.EnvelopeSerializerRegistry;
import xyz.noisif.nsl.codec.envelope.cache.DefaultEnvelopeSerializerCache;
import xyz.noisif.nsl.codec.envelope.cache.EnvelopeSerializerCache;
import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.common.util.CollectionUtil;
import xyz.noisif.nsl.websocket.WsHandshakeRequest;

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
    final String encoding = CollectionUtil.getFirstSafety(req.getQueryParameter(encodingParamName));
    final String frame = CollectionUtil.getFirstSafety(req.getQueryParameter(frameParamName));
    return cache.find(encoding, frame);
  }

  public static class Builder {
    private EnvelopeSerializerRegistry registry;
    private EnvelopeSerializerCache cache = DefaultEnvelopeSerializerCache.createDefault();
    private String encodingParamName = "encoding";
    private String frameParamName = "frame";

    private Builder() {}

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
      Assert.notNull(registry, "registry");
      Assert.notNull(cache, "cache");
      Assert.notNull(encodingParamName, "encodingParamName");
      Assert.notNull(frameParamName, "frameParamName");
      return new QueryParamSerializerResolver(this);
    }
  }
}
