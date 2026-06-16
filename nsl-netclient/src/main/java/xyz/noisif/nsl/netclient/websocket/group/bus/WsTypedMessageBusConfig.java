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

import xyz.noisif.nsl.codec.serialization.TypedMessageSerializer;
import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.net.message.bus.TypedMessageBusListener;
import xyz.noisif.nsl.netclient.websocket.WsClientSession;
import xyz.noisif.nsl.netclient.websocket.WsClientUpgradeRequest;
import xyz.noisif.nsl.netclient.websocket.group.WsClientGroupConfig;
import xyz.noisif.nsl.netclient.websocket.group.codec.WsSessionCodec;
import xyz.noisif.nsl.netclient.websocket.group.codec.WsTypedMessageSessionCodec;

import java.util.function.Consumer;

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
      req.addQueryParameter(dataTypeParamName, typedMessageSerializer.getCodecDataType().getCode());
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
      return self();
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
