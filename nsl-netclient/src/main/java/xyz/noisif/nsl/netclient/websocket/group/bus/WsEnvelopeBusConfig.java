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
package xyz.noisif.nsl.netclient.websocket.group.bus;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import xyz.noisif.nsl.codec.envelope.EnvelopeSerializer;
import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.net.envelope.bus.EnvelopeBusListener;
import xyz.noisif.nsl.netclient.websocket.WsClientSession;
import xyz.noisif.nsl.netclient.websocket.WsClientUpgradeRequest;
import xyz.noisif.nsl.netclient.websocket.group.WsClientGroupConfig;
import xyz.noisif.nsl.netclient.websocket.group.codec.WsEnvelopeSessionCodec;
import xyz.noisif.nsl.netclient.websocket.group.codec.WsSessionCodec;

import java.util.function.Consumer;

public class WsEnvelopeBusConfig extends GenericWsBusConfig {
  private final EnvelopeSerializer<?> envelopeSerializer;

  private WsEnvelopeBusConfig(Builder builder) {
    super(builder);
    envelopeSerializer = builder.envelopeSerializer;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public WsSessionCodec configureProtocol(WsClientUpgradeRequest req) {
    req.addQueryParameter(encodingParamName, envelopeSerializer.getBaseFormat().getFormatName());
    req.addQueryParameter(dataTypeParamName, envelopeSerializer.getCodecDataType().getCode());
    return new WsEnvelopeSessionCodec(envelopeSerializer);
  }

  public static class Builder extends AbstractBuilder<Builder, WsEnvelopeBusConfig> {
    private EnvelopeSerializer<?> envelopeSerializer;

    private Builder() {
      super("encoding", "frame");
    }

    @Override
    protected Builder self() {
      return this;
    }

    @CanIgnoreReturnValue
    public Builder serializer(EnvelopeSerializer<?> envelopeSerializer) {
      this.envelopeSerializer = envelopeSerializer;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder addBusListener(EnvelopeBusListener<WsClientSession> busListener) {
      return super.addRawBusListener(busListener);
    }

    @Override
    public WsEnvelopeBusConfig build() {
      super.validate();
      Assert.notNull(envelopeSerializer, "EnvelopeSerializer cannot be null");
      return new WsEnvelopeBusConfig(this);
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

    public Step envelopeBusConfig(Consumer<Builder> configurer) {
      return super.configure(WsEnvelopeBusConfig.builder(), configurer);
    }
  }
}
