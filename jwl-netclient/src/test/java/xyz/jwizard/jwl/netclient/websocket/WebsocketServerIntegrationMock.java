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

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.common.util.StringUtil;
import xyz.jwizard.jwl.net.NetworkUtil;
import xyz.jwizard.jwl.netclient.TestConstants;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

public class WebsocketServerIntegrationMock extends WebSocketServer {
  private static final Logger LOG = LoggerFactory.getLogger(WebsocketServerIntegrationMock.class);

  public WebsocketServerIntegrationMock(int port) {
    super(new InetSocketAddress(port));
  }

  public WebSocket getSession(DataType dataType) {
    LOG.debug("Searching for session with data type: {}", dataType.getCode());
    return getConnections().stream()
        .filter(c -> StringUtil.toLowerCase(dataType.getCode()).equals(c.getAttachment()))
        .findFirst()
        .orElseThrow(
            () -> {
              LOG.error("Session not found for data type: {}", dataType.getCode());
              return new IllegalStateException("Session not found");
            });
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    final String resource = handshake.getResourceDescriptor();
    LOG.debug("New WebSocket connection attempt: {}", resource);
    final Map<String, String> params = NetworkUtil.getQueryParameters(resource);
    if (!params.containsKey(TestConstants.DATA_TYPE_QUERY_PARAM_NAME)) {
      LOG.warn(
          "Connection rejected: missing {} parameter in query string",
          TestConstants.DATA_TYPE_QUERY_PARAM_NAME);
      throw new IllegalStateException(
          "Missing required parameter: " + TestConstants.DATA_TYPE_QUERY_PARAM_NAME);
    }
    final String frame =
        params.getOrDefault(TestConstants.DATA_TYPE_QUERY_PARAM_NAME, DataType.BINARY.getCode());
    conn.setAttachment(StringUtil.toLowerCase(frame));
    LOG.info(
        "Session {} connected and tagged with frame: {}",
        conn.getRemoteSocketAddress(),
        StringUtil.toLowerCase(frame));
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    LOG.debug(
        "Connection closed: {} (code: {}, remote: {})",
        conn.getRemoteSocketAddress(),
        code,
        remote);
  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    LOG.trace("Received text message from {}: {}", conn.getRemoteSocketAddress(), message);
  }

  @Override
  public void onMessage(WebSocket conn, ByteBuffer message) {
    LOG.trace(
        "Received binary message from {}: {} bytes",
        conn.getRemoteSocketAddress(),
        message.remaining());
  }

  @Override
  public void onError(WebSocket conn, Exception ex) {
    LOG.error(
        "Error occurred on connection {}: {}",
        conn != null ? conn.getRemoteSocketAddress() : "unknown",
        ex.getMessage(),
        ex);
  }

  @Override
  public void onStart() {
    LOG.info("WebSocket mock server started on port {}", getPort());
  }
}
