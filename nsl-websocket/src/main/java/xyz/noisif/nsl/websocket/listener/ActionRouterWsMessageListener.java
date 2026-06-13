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
package xyz.noisif.nsl.websocket.listener;

import xyz.noisif.nsl.codec.envelope.MessageEnvelope;
import xyz.noisif.nsl.codec.envelope.StandardOpCode;
import xyz.noisif.nsl.net.envelope.bus.EnvelopeBusListener;
import xyz.noisif.nsl.websocket.WsSession;
import xyz.noisif.nsl.websocket.listener.action.HeartbeatAction;

public class ActionRouterWsMessageListener extends EnvelopeBusListener<WsSession> {
  private ActionRouterWsMessageListener(Builder builder) {
    super(builder);
    registerManually(new HeartbeatAction());
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected void handleProcessUnknownAction(WsSession session, MessageEnvelope<?> envelope) {
    super.handleProcessUnknownAction(session, envelope);
    session.sendEnvelope(StandardOpCode.UNKNOWN_ACTION);
  }

  @Override
  protected void handleProcessingError(WsSession session, Exception ex) {
    super.handleProcessingError(session, ex);
    session.sendEnvelope(StandardOpCode.INVALID_PAYLOAD);
  }

  public static class Builder extends AbstractBuilder<WsSession, Builder> {
    protected Builder() {}

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public EnvelopeBusListener<WsSession> build() {
      super.validate();
      return new ActionRouterWsMessageListener(this);
    }
  }
}
