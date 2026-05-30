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
package xyz.jwizard.jwl.netclient.websocket.group;

import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.net.http.header.HttpHeaderName;
import xyz.jwizard.jwl.net.http.header.HttpHeaderValue;
import xyz.jwizard.jwl.net.lifecycle.CompositeNetworkSessionLifecycleListener;
import xyz.jwizard.jwl.net.lifecycle.NetworkSessionLifecycleListener;
import xyz.jwizard.jwl.netclient.group.GenericClientGroupConfig;
import xyz.jwizard.jwl.netclient.websocket.WsClientSession;
import xyz.jwizard.jwl.netclient.websocket.auth.CompositeWsClientAuthenticator;
import xyz.jwizard.jwl.netclient.websocket.auth.WsClientAuthenticator;
import xyz.jwizard.jwl.netclient.websocket.group.bus.WsBusConfig;
import xyz.jwizard.jwl.netclient.websocket.group.bus.WsEnvelopeBusConfig;
import xyz.jwizard.jwl.netclient.websocket.group.bus.WsTypedMessageBusConfig;
import xyz.jwizard.jwl.netclient.websocket.group.heartbeat.WsHeartbeatConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WsClientGroupConfig extends GenericClientGroupConfig {
  private final WsClientAuthenticator authenticator;
  private final NetworkSessionLifecycleListener<WsClientSession> lifecycleListener;
  private final Map<HttpHeaderName, String> customHeaders;
  private final WsHeartbeatConfig heartbeatConfig;
  private final WsReconnectConfig reconnectConfig;

  private WsBusConfig busConfig;

  private WsClientGroupConfig(Builder builder) {
    super(builder);
    authenticator = CompositeWsClientAuthenticator.load(builder.authenticators);
    lifecycleListener = CompositeNetworkSessionLifecycleListener.load(builder.componentProvider);
    customHeaders = builder.customHeaders;
    heartbeatConfig = builder.heartbeatConfig;
    reconnectConfig = builder.reconnectConfig;
  }

  public WsClientGroupConfig(Builder builder, WsBusConfig busConfig) {
    this(builder);
    this.busConfig = busConfig;
  }

  public static Builder builder() {
    return new Builder();
  }

  public WsClientAuthenticator getAuthenticator() {
    return authenticator;
  }

  public Map<HttpHeaderName, String> getCustomHeaders() {
    return customHeaders;
  }

  public NetworkSessionLifecycleListener<WsClientSession> getLifecycleListener() {
    return lifecycleListener;
  }

  public WsBusConfig getBusConfig() {
    return busConfig;
  }

  public WsHeartbeatConfig getHeartbeatConfig() {
    return heartbeatConfig;
  }

  public WsReconnectConfig getReconnectConfig() {
    return reconnectConfig;
  }

  public static class Builder extends AbstractBuilder<Builder, WsClientGroupConfig> {
    private final Set<WsClientAuthenticator> authenticators = new HashSet<>();
    private final Map<HttpHeaderName, String> customHeaders = new HashMap<>();
    private ComponentProvider componentProvider;
    private WsHeartbeatConfig heartbeatConfig = null;
    private WsReconnectConfig reconnectConfig = WsReconnectConfig.disabled();

    private Builder() {}

    @Override
    protected Builder self() {
      return this;
    }

    public Builder addAuthenticator(WsClientAuthenticator authenticator) {
      authenticators.add(authenticator);
      return this;
    }

    public WsEnvelopeBusConfig.Step setEnvelopeMode() {
      return new WsEnvelopeBusConfig.Step(this);
    }

    public WsTypedMessageBusConfig.Step setTypedMessageMode() {
      return new WsTypedMessageBusConfig.Step(this);
    }

    public Builder addCustomHeader(HttpHeaderName name, HttpHeaderValue value, Object... args) {
      customHeaders.put(name, value.buildWithArgs(args));
      return this;
    }

    public Builder addCustomHeader(HttpHeaderName name, String value) {
      customHeaders.put(name, value);
      return this;
    }

    public Builder componentProvider(ComponentProvider componentProvider) {
      this.componentProvider = componentProvider;
      return this;
    }

    public Builder heartbeatConfig(WsHeartbeatConfig heartbeatConfig) {
      this.heartbeatConfig = heartbeatConfig;
      return this;
    }

    public Builder reconnectConfig(WsReconnectConfig reconnectConfig) {
      this.reconnectConfig = reconnectConfig;
      return this;
    }

    @Override
    public WsClientGroupConfig build() {
      super.validate();
      Assert.notNull(componentProvider, "ComponentProvider cannot be null");
      return new WsClientGroupConfig(this);
    }
  }
}
