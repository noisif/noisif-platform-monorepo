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
package xyz.jwizard.jwl.netclient.rest.jetty;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.codec.serialization.SerializerRegistry;
import xyz.jwizard.jwl.codec.serialization.json.JacksonSerializer;
import xyz.jwizard.jwl.common.limit.TokenBucketRateLimiter;
import xyz.jwizard.jwl.common.reflect.ClassGraphScanner;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jwl.common.util.math.MemUnit;
import xyz.jwizard.jwl.net.http.HttpStatus;
import xyz.jwizard.jwl.net.http.auth.StandardAuthScheme;
import xyz.jwizard.jwl.net.http.header.CommonHttpHeaderName;
import xyz.jwizard.jwl.net.http.header.CommonHttpHeaderValue;
import xyz.jwizard.jwl.netclient.rest.GenericRestClient;
import xyz.jwizard.jwl.netclient.rest.RestResponse;
import xyz.jwizard.jwl.netclient.rest.TestHttpHeaderName;
import xyz.jwizard.jwl.netclient.rest.TestHttpHeaderValue;
import xyz.jwizard.jwl.netclient.rest.group.RestClientGroupConfig;
import xyz.jwizard.jwl.netclient.rest.group.TestGroup;
import xyz.jwizard.jwl.netclient.rest.intercept.CorrelationInterceptor;
import xyz.jwizard.jwl.netclient.rest.intercept.SignatureInterceptor;

import java.time.Duration;

class JettyRestClientIntegrationTest {
  private static final String USER_AGENT = "JWL-Test-Bot/1.0";

  private static WireMockServer wireMockServer;
  private static ClassScanner scanner;

  private GenericRestClient client;

  @BeforeAll
  static void startWireMock() {
    scanner = new ClassGraphScanner("xyz.jwizard.jwl.netclient.rest");
    wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
    wireMockServer.start();
  }

  @AfterAll
  static void stopWireMock() {
    IoUtil.closeQuietly(wireMockServer, WireMockServer::stop);
    IoUtil.closeQuietly(scanner);
  }

  @BeforeEach
  void setUp() {
    wireMockServer.resetAll();
    configureFor("localhost", wireMockServer.port());
    final String wireMockUrl = "http://localhost:" + wireMockServer.port();

    client =
        JettyRestClient.builder()
            .scanner(scanner)
            .serializerRegistry(
                SerializerRegistry.createDefault()
                    .register(JacksonSerializer.createDefaultStrictMapper()))
            .connectTimeout(Duration.ofSeconds(2))
            .followRedirects(true)
            .maxRedirects(8)
            .maxConnectionsPerHost(10)
            .maxQueuedRequests(1024)
            .maxHeadersSize(8, MemUnit.KB)
            .defaultClientGroup(
                RestClientGroupConfig.builder()
                    .url(wireMockUrl)
                    .principalId(USER_AGENT)
                    .retryOnSafeMethods(3, Duration.ofMillis(500))
                    .build())
            .clientGroup(
                TestGroup.LIMITED_GROUP,
                RestClientGroupConfig.builder()
                    .url(wireMockUrl)
                    .principalId(USER_AGENT)
                    .rateLimit(
                        TokenBucketRateLimiter.builder()
                            .capacity(5)
                            .refillTokens(1)
                            .refillPeriod(Duration.ofHours(1))
                            .build())
                    .build())
            .clientGroup(
                TestGroup.OVERRIDE_GROUP,
                RestClientGroupConfig.builder()
                    .url(wireMockUrl)
                    .principalId(USER_AGENT)
                    .auth(StandardAuthScheme.BASIC, "admin", "secret")
                    .build())
            .build();
    client.start();
  }

  @Test
  @DisplayName("should deserialize JSON response into actual Object")
  void shouldSendGetRequest() {
    // given
    final String url = "/api/user";
    stubFor(
        get(urlEqualTo(url))
            .withHeader("User-Agent", equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.OK_200.getCode())
                    .withHeader(
                        CommonHttpHeaderName.CONTENT_TYPE.getCode(),
                        CommonHttpHeaderValue.APPLICATION_JSON.buildWithArgs())
                    .withBody("{\"id\": 123, \"name\": \"JWizard\"}")));
    final RestResponse<UserDto> response = client.get(url).send(UserDto.class);
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(123);
    assertThat(response.getBody().name()).isEqualTo("JWizard");
  }

  @Test
  @DisplayName("should serialize object to JSON and send via POST (JettyRawBodyStrategy)")
  void shouldSendPostWithRawBody() {
    // given
    final String url = "/api/data";
    stubFor(
        post(urlEqualTo(url))
            .withRequestBody(equalToJson("{\"id\":999,\"name\":\"Test Post\"}"))
            .willReturn(aResponse().withStatus(HttpStatus.CREATED_201.getCode())));
    // when
    final RestResponse<Void> response = client.post(url).body(new UserDto(999, "Test Post")).send();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED_201);
  }

  @Test
  @DisplayName("should apply form parameters (JettyFormBodyStrategy)")
  void shouldSendPostWithFormBody() {
    // given
    final String url = "/api/auth/token";
    stubFor(
        post(urlEqualTo(url))
            .withHeader(
                CommonHttpHeaderName.CONTENT_TYPE.getCode(),
                containing(CommonHttpHeaderValue.APPLICATION_X_WWW_FORM_URLENCODED.buildWithArgs()))
            .withRequestBody(containing("grant_type=client_credentials"))
            .withRequestBody(containing("client_id=12345"))
            .willReturn(aResponse().withStatus(HttpStatus.OK_200.getCode())));
    // when
    final RestResponse<Void> response =
        client
            .post(url)
            .formParam("grant_type", "client_credentials")
            .formParam("client_id", "12345")
            .send();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
  }

  @Test
  @DisplayName("should apply Bearer Authorization and Query Parameters")
  void shouldApplyAuthAndQueryParams() {
    // given
    final String url = "/api/resource";
    final String bearer = "super-secret-token";
    stubFor(
        delete(urlPathEqualTo(url))
            .withQueryParam("force", equalTo("true"))
            .withHeader(
                CommonHttpHeaderName.AUTHORIZATION.getCode(),
                equalTo(StandardAuthScheme.BEARER.buildHeaderValue(bearer)))
            .willReturn(aResponse().withStatus(204)));
    // when
    final RestResponse<Void> response =
        client.delete(url).queryParam("force", "true").bearerAuth(bearer).send();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
  }

  @Test
  @DisplayName("should abort request with 429 when TokenBucketRateLimiter denies permit")
  void shouldAbortWhenRateLimitExceeded() {
    // given
    final String path = "/api/spam";
    final int tokens = 5;
    stubFor(get(urlEqualTo(path)).willReturn(aResponse().withStatus(HttpStatus.OK_200.getCode())));
    for (int i = 0; i < tokens; i++) {
      final RestResponse<Void> okResponse = client.get(path).group(TestGroup.LIMITED_GROUP).send();
      assertThat(okResponse.getStatus().getCode()).isEqualTo(HttpStatus.OK_200.getCode());
    }
    // when
    final RestResponse<Void> rateLimitedResponse =
        client.get(path).group(TestGroup.LIMITED_GROUP).send();
    // then
    assertThat(rateLimitedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS_429);
    verify(tokens, getRequestedFor(urlEqualTo(path)));
  }

  @Test
  @DisplayName("should retry request on 5xx Server Error and succeed on subsequent attempt")
  void shouldRetryOnServerError() {
    // given
    final String path = "/api/retry";
    stubFor(
        get(urlEqualTo(path))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(HttpStatus.BAD_GATEWAY_502.getCode()))
            .willSetStateTo("Second Call"));
    stubFor(
        get(urlEqualTo(path))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("Second Call")
            .willReturn(aResponse().withStatus(HttpStatus.OK_200.getCode())));
    // when
    final RestResponse<Void> response = client.get("/api/retry").send();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
    verify(2, getRequestedFor(urlEqualTo(path)));
  }

  @Test
  @DisplayName("should apply standalone interceptor adding dynamic header and query param")
  void shouldApplyStandaloneInterceptorAddingHeadersAndQueryParams() {
    // given
    final String path = "/api/dynamic";
    final String firstParam = "999";
    final String secondParam = "ABC";
    stubFor(
        get(urlPathEqualTo(path))
            .withHeader(
                TestHttpHeaderName.X_CORRELATION_ID.getCode(),
                equalTo(TestHttpHeaderValue.REQ.buildWithArgs(firstParam, secondParam)))
            .withQueryParam("tracking_enabled", equalTo("true"))
            .withHeader("User-Agent", containing(USER_AGENT))
            .willReturn(aResponse().withStatus(HttpStatus.OK_200.getCode())));
    // when
    final RestResponse<Void> response =
        client.get(path).interceptor(new CorrelationInterceptor(firstParam, secondParam)).send();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
    verify(
        1,
        getRequestedFor(urlPathEqualTo(path))
            .withHeader(
                TestHttpHeaderName.X_CORRELATION_ID.getCode(),
                equalTo(TestHttpHeaderValue.REQ.buildWithArgs(firstParam, secondParam)))
            .withQueryParam("tracking_enabled", equalTo("true")));
  }

  @Test
  @DisplayName("should read headers and query params from RequestView and apply signature")
  void shouldReadRequestViewAndAddSignatureHeader() {
    // given
    final String path = "/api/secure/action";
    final String firstParam = "UPDATE";
    final String secondParam = "505";
    stubFor(
        get(urlPathEqualTo(path))
            .withHeader(
                TestHttpHeaderName.X_ACTION_TYPE.getCode(),
                equalTo(TestHttpHeaderValue.UPDATE.buildWithArgs()))
            .withQueryParam("target_id", equalTo(secondParam))
            .withHeader(
                TestHttpHeaderName.X_REQUEST_SIGNATURE.getCode(),
                equalTo(TestHttpHeaderValue.SIG.buildWithArgs(firstParam, secondParam)))
            .willReturn(aResponse().withStatus(HttpStatus.OK_200.getCode())));
    // when
    final RestResponse<Void> response =
        client
            .get(path)
            .header(TestHttpHeaderName.X_ACTION_TYPE, TestHttpHeaderValue.UPDATE)
            .queryParam("target_id", secondParam)
            .interceptor(new SignatureInterceptor())
            .send();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
    verify(
        1,
        getRequestedFor(urlPathEqualTo(path))
            .withHeader(TestHttpHeaderName.X_ACTION_TYPE.getCode(), equalTo(firstParam))
            .withQueryParam("target_id", equalTo(secondParam))
            .withHeader(
                TestHttpHeaderName.X_REQUEST_SIGNATURE.getCode(),
                equalTo(TestHttpHeaderValue.SIG.buildWithArgs(firstParam, secondParam))));
  }

  @Test
  @DisplayName("should override group Basic Auth with request-specific Bearer Auth")
  void shouldOverridePoolAuthWithRequestAuth() {
    // given
    final String path = "/api/secure";
    final String bearer = "request-token";
    stubFor(
        get(urlEqualTo(path))
            .withHeader(
                CommonHttpHeaderName.AUTHORIZATION.getCode(),
                equalTo(StandardAuthScheme.BEARER.buildHeaderValue(bearer)))
            .willReturn(aResponse().withStatus(HttpStatus.OK_200.getCode())));
    // when
    final RestResponse<Void> response =
        client.get(path).group(TestGroup.OVERRIDE_GROUP).bearerAuth(bearer).send();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
    verify(
        getRequestedFor(urlEqualTo(path))
            .withHeader(
                CommonHttpHeaderName.AUTHORIZATION.getCode(),
                equalTo(StandardAuthScheme.BEARER.buildHeaderValue(bearer))));
  }

  @Test
  @DisplayName("should NOT retry when retry is explicitly disabled at request level")
  void shouldDisableRetryAtRequestLevel() {
    // given
    final String path = "/api/no-retry";
    stubFor(
        get(urlEqualTo(path))
            .willReturn(aResponse().withStatus(HttpStatus.BAD_GATEWAY_502.getCode())));
    // when
    final RestResponse<Void> response = client.get(path).disableRetry().send();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY_502);
    verify(1, getRequestedFor(urlEqualTo(path)));
  }

  @Test
  @DisplayName("should strictly obey request-level retry limit and ignore higher group limit")
  void shouldObeyRequestLevelRetryLimit() {
    // given
    final String path = "/api/fail-fast";
    stubFor(
        get(urlEqualTo(path))
            .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR_500.getCode())));
    // when
    final RestResponse<Void> response = client.get(path).retry(1, Duration.ofMillis(10)).send();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500);
    verify(2, getRequestedFor(urlEqualTo(path)));
  }
}

record UserDto(int id, String name) {}
