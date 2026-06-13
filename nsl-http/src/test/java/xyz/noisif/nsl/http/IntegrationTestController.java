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
package xyz.noisif.nsl.http;

import xyz.noisif.nsl.common.util.math.MemUnit;
import xyz.noisif.nsl.http.annotation.Body;
import xyz.noisif.nsl.http.annotation.HttpController;
import xyz.noisif.nsl.http.annotation.PathVariable;
import xyz.noisif.nsl.http.annotation.RequestMapping;
import xyz.noisif.nsl.http.annotation.RequestParam;
import xyz.noisif.nsl.http.annotation.SecuredRoute;
import xyz.noisif.nsl.http.header.TestHttpHeaderName;
import xyz.noisif.nsl.net.http.HttpMethod;

import java.util.Map;

@HttpController
class IntegrationTestController {
  @RequestMapping(value = "/api/test", method = HttpMethod.POST)
  ResponseEntity<String> handleTest(@Body TestEnvelope testEnvelope) {
    return ResponseEntity.ok("Success: " + testEnvelope.testUser().name());
  }

  @RequestMapping(value = "/api/raw", method = HttpMethod.POST)
  ResponseEntity<String> handleRawData(@Body byte[] data) {
    return ResponseEntity.ok("Received " + data.length + " bytes of raw data");
  }

  @RequestMapping(value = "/api/limited", method = HttpMethod.POST)
  ResponseEntity<String> handleLimitedData(@Body(limit = 10, unit = MemUnit.BYTES) byte[] data) {
    return ResponseEntity.ok("Processed limited payload");
  }

  @RequestMapping(value = "/api/users/{id}", method = HttpMethod.GET)
  ResponseEntity<String> getUserById(@PathVariable("id") String userId) {
    return ResponseEntity.ok("User ID: " + userId);
  }

  @RequestMapping(value = "/api/search", method = HttpMethod.GET)
  ResponseEntity<String> search(
      @RequestParam("q") String query, @RequestParam("page") Integer page) {
    return ResponseEntity.ok("Search: " + query + " on page " + page);
  }

  @RequestMapping(value = "/api/users/{id}/orders", method = HttpMethod.GET)
  ResponseEntity<String> getUserOrders(
      @PathVariable("id") String userId, @RequestParam("status") String status) {
    return ResponseEntity.ok("User " + userId + " orders with status " + status);
  }

  @RequestMapping(value = "/api/products", method = HttpMethod.GET)
  ResponseEntity<String> getProducts(
      @RequestParam(value = "category", defaultValue = "all") String category) {
    return ResponseEntity.ok("Category: " + category);
  }

  @RequestMapping(value = "/api/profile", method = HttpMethod.GET)
  ResponseEntity<String> getProfile(@RequestParam(value = "token", required = false) String token) {
    final String status = (token == null) ? "Guest" : "User-" + token;
    return ResponseEntity.ok("Status: " + status);
  }

  @RequestMapping(value = "/api/items", method = HttpMethod.GET)
  ResponseEntity<String> getItems(
      @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
    return ResponseEntity.ok("Limit: " + limit);
  }

  @RequestMapping(value = "/api/map", method = HttpMethod.GET)
  ResponseEntity<Map<String, Object>> getMap() {
    return ResponseEntity.ok(
        Map.of(
            "status", "UP",
            "version", "1.0.0",
            "active", true));
  }

  @RequestMapping(value = "/api/blocked", method = HttpMethod.GET)
  ResponseEntity<String> blockedEndpoint() {
    return ResponseEntity.ok("blocked");
  }

  @RequestMapping(value = "/api/public", method = HttpMethod.GET)
  ResponseEntity<String> openEndpoint() {
    return ResponseEntity.ok("public data");
  }

  @SecuredRoute
  @RequestMapping(value = "/api/private", method = HttpMethod.GET)
  ResponseEntity<String> secureEndpoint() {
    return ResponseEntity.ok("secret data");
  }

  @RequestMapping(value = "/api/inspect", method = HttpMethod.GET)
  ResponseEntity<String> inspectRequest(HttpRequest request) {
    final String customHeader = request.getHeader(TestHttpHeaderName.X_INSPECT_HEADER);
    final String method = request.getMethod();
    return ResponseEntity.ok(
        String.format(
            "method: %s, header: %s", method, customHeader != null ? customHeader : "NONE"));
  }
}
