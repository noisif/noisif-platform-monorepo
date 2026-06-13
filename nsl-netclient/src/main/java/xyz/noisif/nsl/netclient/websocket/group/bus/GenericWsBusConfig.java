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

import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.net.bus.CompositeBusListener;
import xyz.noisif.nsl.net.bus.RawBusListener;
import xyz.noisif.nsl.netclient.websocket.WsClientSession;
import xyz.noisif.nsl.netclient.websocket.group.WsClientGroupConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

  abstract static class AbstractBuilder<
      B extends AbstractBuilder<B, C>, C extends GenericWsBusConfig> {
    protected final List<RawBusListener<WsClientSession>> busListeners = new ArrayList<>();
    private final boolean paramsRequired;
    protected String encodingParamName;
    protected String dataTypeParamName;

    protected AbstractBuilder(
        String encodingParamName, String dataTypeParamName, boolean paramsRequired) {
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

  abstract static class AbstractStep<
      S extends AbstractStep<S, B>, B extends AbstractBuilder<B, ?>> {
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
