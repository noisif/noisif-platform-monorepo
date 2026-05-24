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
package xyz.jwizard.jwl.http.jetty;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.codec.serialization.SerializerRegistry;
import xyz.jwizard.jwl.codec.serialization.raw.RawByteSerializer;
import xyz.jwizard.jwl.common.di.ApplicationContext;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.di.GuiceComponentProvider;
import xyz.jwizard.jwl.common.reflect.ClassGraphScanner;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.common.util.StringUtil;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.http.HttpServer;
import xyz.jwizard.jwl.http.TestConstants;
import xyz.jwizard.jwl.http.TestEnvelope;
import xyz.jwizard.jwl.http.TestUser;
import xyz.jwizard.jwl.http.filter.CacheSpyFilter;
import xyz.jwizard.jwl.http.header.TestHttpHeaderName;
import xyz.jwizard.jwl.http.header.TestHttpHeaderValue;
import xyz.jwizard.jwl.net.http.HttpStatus;
import xyz.jwizard.jwl.net.http.header.CommonHttpHeaderName;

public class JettyHttpServerIntegrationTest {
    private static HttpServer httpServer;
    private static int dynamicPort;
    private static ClassScanner scanner;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeAll
    static void startServer() {
        scanner = new ClassGraphScanner("xyz.jwizard.jwl.http");
        final ApplicationContext context = ApplicationContext.createDefault(scanner, Map.of(
            ComponentProvider.class, GuiceComponentProvider.class
        ));
        httpServer = JettyHttpServer.builder()
            .componentProvider(context.getComponentProvider())
            .serializerRegistry(SerializerRegistry.createDefault()
                .register(TestConstants.SERIALIZER)
                .register(RawByteSerializer.createDefault())
            )
            .port(0)
            .build();
        httpServer.start();
        dynamicPort = httpServer.getLocalPort();
    }

    @AfterAll
    static void stopServer() {
        httpServer.close();
        IoUtil.closeQuietly(scanner);
    }

    private HttpResponse<String> get(String path) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + dynamicPort + path))
            .GET()
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(Object body) throws Exception {
        final String json = TestConstants.SERIALIZER.serialize(body);
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + dynamicPort + "/api/test"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("POST /api/test should return 200 OK when valid JSON is sent")
    void shouldReturnOkForValidRequest() throws Exception {
        // given
        final TestEnvelope payload = new TestEnvelope("REQ-001", new TestUser("JWizard", 25));
        // when
        final HttpResponse<String> response = post(payload);
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        assertThat(response.body()).contains("Success: JWizard");
    }

    @Test
    @DisplayName("POST /api/test should return 400 Bad Request when validation fails (age < 18)")
    void shouldReturn400ForInvalidData() throws Exception {
        // given
        final TestEnvelope payload = new TestEnvelope("REQ-002", new TestUser("Jo", 10));
        // when
        final HttpResponse<String> response = post(payload);
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST_400.getCode());
        assertThat(response.body()).isEmpty();
    }

    @Test
    @DisplayName("GET /non-existing should return 404 Not Found for non-existing route")
    void shouldReturn404() throws Exception {
        // given & when
        final HttpResponse<String> response = get("/non-existing");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND_404.getCode());
    }

    @Test
    @DisplayName("GET /api/users/{id} should resolve path variable")
    void shouldResolvePathVariable() throws Exception {
        // given & when
        final HttpResponse<String> response = get("/api/users/12345");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        assertThat(response.body()).isEqualTo("User ID: 12345");
    }

    @Test
    @DisplayName("GET /api/search should resolve multiple query parameters")
    void shouldResolveQueryParams() throws Exception {
        // given & when
        final HttpResponse<String> response = get("/api/search?q=java&page=1");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        assertThat(response.body()).isEqualTo("Search: java on page 1");
    }

    @Test
    @DisplayName("GET /api/products should use default value 'all' when param is missing")
    void shouldUseCaseDefaultValue() throws Exception {
        // given & when
        final HttpResponse<String> response = get("/api/products");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        assertThat(response.body()).isEqualTo("Category: all");
    }

    @Test
    @DisplayName("GET /api/profile should allow null when required is false")
    void shouldAllowOptionalParam() throws Exception {
        // given & when
        final HttpResponse<String> response = get("/api/profile");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        assertThat(response.body()).isEqualTo("Status: Guest");
    }

    @Test
    @DisplayName("GET /api/items should parse default string '10' to int")
    void shouldParseDefaultInt() throws Exception {
        // given & when
        final HttpResponse<String> response = get("/api/items");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        assertThat(response.body()).isEqualTo("Limit: 10");
    }

    @Test
    @DisplayName("GET /api/map should return 200 OK and serialize Map to JSON")
    void shouldReturnMapAsJson() throws Exception {
        // given & when
        final HttpResponse<String> response = get("/api/map");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        final String body = response.body();
        assertThat(body).isNotBlank();
        assertThat(body).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("GET /api/map should contain custom header added by HttpFilter")
    void shouldExecuteFilterAndAddHeader() throws Exception {
        // given & when
        final HttpResponse<String> response = get("/api/map");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        assertThat(response.headers().firstValue(TestHttpHeaderName.X_TEST_FILTER.getCode()))
            .isPresent().contains(TestHttpHeaderValue.EXECUTED.buildWithArgs());
    }

    @Test
    @DisplayName("GET /api/blocked should be intercepted and stopped by HttpFilter returning false")
    void shouldBlockRequestByFilter() throws Exception {
        // given & when
        final HttpResponse<String> response = get("/api/blocked");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN_403.getCode());
        assertThat(response.body()).isEmpty();
        assertThat(response.headers().firstValue(TestHttpHeaderName.X_TEST_FILTER.getCode()))
            .isPresent().contains(TestHttpHeaderValue.EXECUTED.buildWithArgs());
    }

    @Test
    @DisplayName("GET /api/public should completely bypass AnnotationSecurityFilter")
    void shouldBypassFilterForUnannotatedRoute() throws Exception {
        // given & when
        final HttpResponse<String> response = get("/api/public");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        assertThat(response.body()).isEqualTo("public data");
        assertThat(response.headers().firstValue(TestHttpHeaderName.X_SECURED_BY.getCode()))
            .isEmpty();
    }

    @Test
    @DisplayName("GET /api/private should return 401 Unauthorized when token is missing")
    void shouldBlockSecuredRouteWithoutToken() throws Exception {
        // given & when
        final HttpResponse<String> response = get("/api/private");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED_401.getCode());
        assertThat(response.body()).isEmpty();
    }

    @Test
    @DisplayName("GET /api/private should return 200 OK when valid token is provided")
    void shouldAllowSecuredRouteWithToken() throws Exception {
        // given
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + dynamicPort + "/api/private"))
            .header(CommonHttpHeaderName.AUTHORIZATION.getCode(), TestConstants.TEST_PASSWORD)
            .GET()
            .build();
        // when
        final HttpResponse<String> response = httpClient
            .send(request, HttpResponse.BodyHandlers.ofString());
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        assertThat(response.body()).isEqualTo("secret data");
        assertThat(response.headers().firstValue(TestHttpHeaderName.X_SECURED_BY.getCode()))
            .isPresent().contains(TestHttpHeaderValue.ANNOTATION_FILTER.buildWithArgs());
    }

    @Test
    @DisplayName("filters should be executed in order based on their priority")
    void shouldExecuteFiltersInCorrectOrder() throws Exception {
        // given & when
        final HttpResponse<String> response = get("/api/public");
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        assertThat(response.headers().firstValue(TestHttpHeaderName.X_FILTER_ORDER.getCode()))
            .isPresent()
            .contains("First -> Second");
    }

    @Test
    @DisplayName("filter should be resolved via supports() only once and then cached")
    void shouldCacheFilterSupportResult() throws Exception {
        CacheSpyFilter.supportsCounter.set(0);
        // given & when
        get("/api/map");
        final int countAfterFirstRequest = CacheSpyFilter.supportsCounter.get();
        assertThat(countAfterFirstRequest).isEqualTo(1);
        get("/api/map");
        final int countAfterSecondRequest = CacheSpyFilter.supportsCounter.get();
        // then
        assertThat(countAfterSecondRequest)
            .withFailMessage("Filter supports() was called again, cache is not working")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/raw should accept wildcard Content-Type (image/png) and resolve as RAW")
    void shouldAcceptWildcardContentType() throws Exception {
        // given
        final byte[] payload = new byte[]{0x01, 0x02, 0x03, 0x04};
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + dynamicPort + "/api/raw"))
            .header("Content-Type", "image/png")
            .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
            .build();
        // when
        final HttpResponse<String> response = httpClient
            .send(request, HttpResponse.BodyHandlers.ofString());
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        assertThat(response.body()).isEqualTo("Received 4 bytes of raw data");
    }

    @Test
    @DisplayName("POST /api/limited should reject payload explicitly exceeding @Body " +
        "limit (Fast-Fail via Content-Length)")
    void shouldFailFastOnExceedingDeclaredContentLength() throws Exception {
        // given
        final byte[] payload = StringUtil.getBytes("123456789012345");
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + dynamicPort + "/api/limited"))
            .header("Content-Type", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofByteArray(payload)) // Set's Content-Length: 15
            .build();
        // when
        final HttpResponse<String> response = httpClient
            .send(request, HttpResponse.BodyHandlers.ofString());
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE_413.getCode());
    }

    @Test
    @DisplayName("POST /api/limited should reject chunked payload exceeding limit " +
        "(via LimitedInputStream)")
    void shouldFailViaLimitedInputStreamForChunkedTransfer() throws Exception {
        // given - endpoint has limit of 10 bytes, we send 15 bytes of random garbage
        // via chunked transfer
        final byte[] payload = new byte[15];
        ThreadLocalRandom.current().nextBytes(payload);
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + dynamicPort + "/api/limited"))
            .header("Content-Type", "application/octet-stream")
            // ofInputStream forces chunked transfer encoding, meaning Content-Length is not sent
            // this bypasses the fail-fast check and forces LimitedInputStream to throw exception
            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(payload)))
            .build();
        // when
        final HttpResponse<String> response = httpClient
            .send(request, HttpResponse.BodyHandlers.ofString());
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE_413.getCode());
    }

    @Test
    @DisplayName("GET /api/inspect should inject HttpRequest directly without any annotations")
    void shouldInjectHttpRequestDirectly() throws Exception {
        // given
        final String headerValue = TestHttpHeaderValue.DIRECT_INJECT.buildWithArgs();
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + dynamicPort + "/api/inspect"))
            .header(TestHttpHeaderName.X_INSPECT_HEADER.getCode(), headerValue)
            .GET()
            .build();
        // when
        final HttpResponse<String> response = httpClient
            .send(request, HttpResponse.BodyHandlers.ofString());
        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200.getCode());
        assertThat(response.body()).isEqualTo("method: GET, header: %s", headerValue);
    }
}
