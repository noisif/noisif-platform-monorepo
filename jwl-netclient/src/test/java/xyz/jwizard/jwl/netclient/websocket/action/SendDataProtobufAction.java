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
package xyz.jwizard.jwl.netclient.websocket.action;

import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.net.envelope.EnvelopeAction;
import xyz.jwizard.jwl.netclient.websocket.TestWsOpCode;
import xyz.jwizard.jwl.netclient.websocket.protobuf.TestPayloadProto;
import xyz.jwizard.jwl.websocket.WsSession;

import jakarta.inject.Singleton;

@Singleton
public class SendDataProtobufAction
    implements EnvelopeAction<WsSession, TestPayloadProto.MyMessage> {
  @Override
  public void handle(WsSession channel, TestPayloadProto.MyMessage data) {
    final TestPayloadProto.MyMessage response =
        data.toBuilder().setContent("Received: " + data.getContent()).build();
    channel.sendEnvelope(TestWsOpCode.RECEIVE_DATA_PROTO, response);
  }

  @Override
  public OpCode opCode() {
    return TestWsOpCode.SEND_DATA_PROTO;
  }

  @Override
  public Class<TestPayloadProto.MyMessage> payloadClass() {
    return TestPayloadProto.MyMessage.class;
  }
}
