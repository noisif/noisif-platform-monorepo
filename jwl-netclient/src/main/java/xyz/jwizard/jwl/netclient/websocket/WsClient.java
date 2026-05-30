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
package xyz.jwizard.jwl.netclient.websocket;

import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.netclient.group.ClientGroup;

public interface WsClient {
  void send(ClientGroup clientGroup, byte[] message);

  void send(ClientGroup clientGroup, String message);

  void sendObject(ClientGroup clientGroup, Object object);

  void sendEnvelope(ClientGroup clientGroup, OpCode opCode, Object data);

  default void send(byte[] message) {
    send(ClientGroup.GLOBAL, message);
  }

  default void send(String message) {
    send(ClientGroup.GLOBAL, message);
  }

  default void sendEnvelope(OpCode opCode, Object data) {
    sendEnvelope(ClientGroup.GLOBAL, opCode, data);
  }

  default void sendEnvelope(ClientGroup clientGroup, OpCode opCode) {
    sendEnvelope(clientGroup, opCode, null);
  }

  default void sendEnvelope(OpCode opCode) {
    sendEnvelope(ClientGroup.GLOBAL, opCode, null);
  }

  boolean isConnected(ClientGroup group);

  default boolean isConnected() {
    return isConnected(ClientGroup.GLOBAL);
  }
}
