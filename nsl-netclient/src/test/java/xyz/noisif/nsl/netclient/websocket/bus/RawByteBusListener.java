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
package xyz.noisif.nsl.netclient.websocket.bus;

import xyz.noisif.nsl.net.message.bus.TypedMessageBusListener;
import xyz.noisif.nsl.netclient.websocket.TestQueueProvider;
import xyz.noisif.nsl.netclient.websocket.WsClientSession;

public class RawByteBusListener extends TypedMessageBusListener<byte[], WsClientSession> {
  private final TestQueueProvider testQueueProvider;

  public RawByteBusListener(TestQueueProvider testQueueProvider) {
    this.testQueueProvider = testQueueProvider;
  }

  @Override
  protected void handle(WsClientSession session, byte[] message) {
    testQueueProvider.get().add(message);
  }

  @Override
  protected Class<byte[]> getTargetType() {
    return byte[].class;
  }
}
