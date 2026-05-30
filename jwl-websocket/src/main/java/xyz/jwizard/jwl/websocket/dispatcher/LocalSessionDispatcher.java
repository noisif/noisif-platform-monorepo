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
package xyz.jwizard.jwl.websocket.dispatcher;

import xyz.jwizard.jwl.websocket.broadcast.WsMessageSink;
import xyz.jwizard.jwl.websocket.broadcast.WsTopic;

public interface LocalSessionDispatcher extends WsMessageSink {
  void dispatchRaw(String topic, byte[] payload);

  void dispatchRawAll(byte[] payload);

  @Override
  default void payload(WsTopic topic, byte[] payload) {
    dispatchRaw(topic.getTopic(), payload);
  }

  @Override
  default void payloadAll(byte[] payload) {
    dispatchRawAll(payload);
  }
}
