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
package xyz.noisif.nsl.serve.undertow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.noisif.nsl.common.util.io.IoUtil;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

class UndertowDevServerIntegrationTest {
  private static final String HTTP_CONTENT = "<h1>Hello SPA</h1>";
  private static final String BACKEND_HTTP_RESPONSE = "BACKEND_HTTP_RESPONSE";
  private static final String STRIPPED_BACKEND_RESPONSE = "STRIPPED_BACKEND_RESPONSE";

  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

  private static int backendPort;
  private static int devServerPort;
  private static Undertow mockBackendServer;
  private static UndertowDevServer devServer;
  private static Path tempDistDir;
  private static File configFile;

  @BeforeAll
  static void setup() throws Exception {
    backendPort = startMockBackend();
    tempDistDir = prepareHtmlFile();
    configFile = prepareConfigFile();

    devServer = new UndertowDevServer(new String[] {"--config", configFile.getAbsolutePath()});
    new Thread(devServer::start).start();

    devServerPort = devServer.getServerReadyFuture().get(5, TimeUnit.SECONDS);
  }

  @AfterAll
  static void teardown() throws Exception {
    IoUtil.closeQuietly(devServer, UndertowDevServer::onServerStopped);
    IoUtil.closeQuietly(mockBackendServer, Undertow::stop);
    Files.deleteIfExists(tempDistDir.resolve("index.html"));
    Files.deleteIfExists(tempDistDir);
    Files.deleteIfExists(configFile.toPath());
  }

  @Test
  @DisplayName("should serve static index.html file")
  void shouldServeStaticIndexHtml() throws Exception {
    // given
    final HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + devServerPort + "/index.html"))
            .GET()
            .build();
    // when
    final HttpResponse<String> response =
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    // then
    assertEquals(StatusCodes.OK, response.statusCode(), "Status code should be 200");
    assertEquals(
        HTTP_CONTENT, response.body(), "Response body should match the static file content");
  }

  @Test
  @DisplayName("should fallback to index.html for unknown SPA routes")
  void shouldFallbackToIndexHtmlForSpaRoutes() throws Exception {
    // given
    final HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + devServerPort + "/dashboard/users"))
            .GET()
            .build();
    // when
    final HttpResponse<String> response =
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    // then
    assertEquals(StatusCodes.OK, response.statusCode(), "Should return 200 despite missing file");
    assertEquals(HTTP_CONTENT, response.body(), "Should return index.html content (SPA Fallback)");
  }

  @Test
  @DisplayName(
      "should proxy HTTP requests to backend server preserving prefix "
          + "when stripPrefix is false")
  void shouldProxyHttpRequestsToBackend() throws Exception {
    // given
    final HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + devServerPort + "/api/data"))
            .GET()
            .build();
    // when
    final HttpResponse<String> response =
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    // then
    assertEquals(StatusCodes.OK, response.statusCode(), "Status code should be 200");
    assertEquals(
        BACKEND_HTTP_RESPONSE,
        response.body(),
        "Request should be proxied to the mock backend with /api prefix");
  }

  @Test
  @DisplayName("should proxy HTTP requests and strip prefix when stripPrefix is true")
  void shouldProxyAndStripPrefixWhenEnabled() throws Exception {
    // given
    final HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + devServerPort + "/stripped-api/stripped-data"))
            .GET()
            .build();
    // when
    final HttpResponse<String> response =
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    // then
    assertEquals(StatusCodes.OK, response.statusCode(), "Status code should be 200");
    assertEquals(
        STRIPPED_BACKEND_RESPONSE,
        response.body(),
        "Request should be proxied to /stripped-data on backend without /stripped-api prefix");
  }

  @Test
  @DisplayName("should proxy WebSocket connections to backend server")
  void shouldProxyWebSocketConnections() throws Exception {
    // given
    final CompletableFuture<String> receivedMessageFuture = new CompletableFuture<>();
    final WebSocket webSocket =
        HTTP_CLIENT
            .newWebSocketBuilder()
            .buildAsync(
                URI.create("ws://localhost:" + devServerPort + "/api/ws"),
                new WebSocket.Listener() {
                  @Override
                  public CompletionStage<?> onText(
                      WebSocket webSocket, CharSequence data, boolean last) {
                    receivedMessageFuture.complete(data.toString());
                    return null;
                  }
                })
            .get(5, TimeUnit.SECONDS);
    // when
    final CompletableFuture<?> ignored = webSocket.sendText("PING_NSL", true);
    final String response = receivedMessageFuture.get(5, TimeUnit.SECONDS);
    // then
    assertEquals(
        "ECHO: PING_NSL", response, "WebSocket message should be echoed by the backend via proxy");
  }

  private static int startMockBackend() {
    mockBackendServer =
        Undertow.builder()
            .addHttpListener(0, "localhost")
            .setHandler(
                Handlers.path()
                    // HTTP
                    .addExactPath(
                        "/api/data",
                        exchange -> {
                          exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                          exchange.getResponseSender().send(BACKEND_HTTP_RESPONSE);
                        })
                    .addExactPath(
                        "/stripped-data",
                        exchange -> {
                          exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                          exchange.getResponseSender().send(STRIPPED_BACKEND_RESPONSE);
                        })
                    // WS
                    .addExactPath(
                        "/api/ws",
                        Handlers.websocket(
                            (exchange, channel) -> {
                              channel
                                  .getReceiveSetter()
                                  .set(
                                      new AbstractReceiveListener() {
                                        @Override
                                        protected void onFullTextMessage(
                                            WebSocketChannel channel, BufferedTextMessage message) {
                                          WebSockets.sendText(
                                              "ECHO: " + message.getData(), channel, null);
                                        }
                                      });
                              channel.resumeReceives();
                            })))
            .build();
    mockBackendServer.start();
    return mockBackendServer.getListenerInfo().stream()
        .map(info -> ((InetSocketAddress) info.getAddress()).getPort())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Failed to retrieve mock backend port"));
  }

  private static Path prepareHtmlFile() throws Exception {
    final Path tempDistDir = Files.createTempDirectory("nsl-dist-test");
    Files.writeString(tempDistDir.resolve("index.html"), HTTP_CONTENT);
    return tempDistDir;
  }

  private static File prepareConfigFile() throws Exception {
    final File configFile = Files.createTempFile("dev-server", ".properties").toFile();
    final Properties props = new Properties();
    props.setProperty("server.host", "0.0.0.0");
    props.setProperty("server.port", "0");
    props.setProperty("server.dir", tempDistDir.toAbsolutePath().toString());
    // /api/data -> /api/data
    props.setProperty("proxy.0.prefix", "/api");
    props.setProperty("proxy.0.targetUrl", "http://localhost:" + backendPort);
    props.setProperty("proxy.0.stripPrefix", "false");
    // /stripped-api/stripped-data -> /stripped-data
    props.setProperty("proxy.1.prefix", "/stripped-api");
    props.setProperty("proxy.1.targetUrl", "http://localhost:" + backendPort);
    props.setProperty("proxy.1.stripPrefix", "true");
    try (final FileOutputStream fos = new FileOutputStream(configFile)) {
      props.store(fos, "Test Config");
    }
    return configFile;
  }
}
