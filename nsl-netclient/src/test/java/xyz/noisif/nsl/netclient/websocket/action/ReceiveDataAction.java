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
package xyz.noisif.nsl.netclient.websocket.action;

import xyz.noisif.nsl.codec.envelope.OpCode;
import xyz.noisif.nsl.net.envelope.EnvelopeAction;
import xyz.noisif.nsl.netclient.websocket.TestQueueProvider;
import xyz.noisif.nsl.netclient.websocket.TestWsOpCode;
import xyz.noisif.nsl.netclient.websocket.WsClientSession;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ReceiveDataAction implements EnvelopeAction<WsClientSession, String> {
  private final TestQueueProvider testQueueProvider;

  @Inject
  public ReceiveDataAction(TestQueueProvider testQueueProvider) {
    this.testQueueProvider = testQueueProvider;
  }

  @Override
  public void handle(WsClientSession channel, String data) {
    testQueueProvider.get().add(data);
  }

  @Override
  public OpCode opCode() {
    return TestWsOpCode.RECEIVE_DATA;
  }

  @Override
  public Class<String> payloadClass() {
    return String.class;
  }
}
