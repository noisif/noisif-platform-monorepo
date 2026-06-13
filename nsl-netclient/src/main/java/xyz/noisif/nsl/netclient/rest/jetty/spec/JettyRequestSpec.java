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
package xyz.noisif.nsl.netclient.rest.jetty.spec;

import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.http.HttpField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.codec.serialization.MessageSerializer;
import xyz.noisif.nsl.codec.serialization.SerializerRegistry;
import xyz.noisif.nsl.common.bootstrap.CriticalBootstrapException;
import xyz.noisif.nsl.common.reflect.ClassScanner;
import xyz.noisif.nsl.net.http.HttpMethod;
import xyz.noisif.nsl.netclient.group.ClientRegistry;
import xyz.noisif.nsl.netclient.rest.RestRequestException;
import xyz.noisif.nsl.netclient.rest.RestResponse;
import xyz.noisif.nsl.netclient.rest.group.RestClientGroupConfig;
import xyz.noisif.nsl.netclient.rest.spec.GenericRequestSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class JettyRequestSpec extends GenericRequestSpec {
  private static final Logger LOG = LoggerFactory.getLogger(JettyRequestSpec.class);

  private final HttpClient client;
  private final List<JettyBodyStrategy> bodyStrategies;

  public JettyRequestSpec(
      HttpClient client,
      ClientRegistry<RestClientGroupConfig> clientRegistry,
      String url,
      HttpMethod method,
      SerializerRegistry<MessageSerializer> serializerRegistry,
      ClassScanner scanner) {
    super(clientRegistry, url, method, serializerRegistry);
    this.client = client;
    bodyStrategies = loadRequestBodyStrategies(scanner);
  }

  @Override
  public <T> RestResponse<T> onSend(Class<T> responseType) {
    Request request = null;
    final String fullUri = resolveFullUrl();
    try {
      LOG.debug("Sending {} request to: {}", method.name(), fullUri);
      request = client.newRequest(fullUri).method(method.name());
      for (final Map.Entry<String, String> entry : headers.entrySet()) {
        request.headers(h -> h.add(entry.getKey(), entry.getValue()));
      }
      for (final Map.Entry<String, String> entry : queryParams.entrySet()) {
        request.param(entry.getKey(), entry.getValue());
      }
      if (requestTimeout != null) {
        request.timeout(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
      }
      boolean strategyApplied = false;
      for (final JettyBodyStrategy strategy : bodyStrategies) {
        if (strategy.supports(this)) {
          LOG.trace("Using body strategy: {}", strategy.getClass().getSimpleName());
          request.body(
              strategy.buildContent(this, messageSerializer, new JettyHeaderConsumer(request)));
          strategyApplied = true;
          break;
        }
      }
      if (!strategyApplied && body != null) {
        LOG.warn(
            "Body is present but no suitable JettyBodyStrategy was found for " + "request to: {}",
            fullUri);
      }
      final ContentResponse response = request.send();
      LOG.debug("Received response from {}: status={}", fullUri, response.getStatus());
      return new RestResponse<>(
          response.getStatus(),
          extractHeaders(response),
          parseResponseBody(response.getContent(), responseType));
    } catch (Exception ex) {
      LOG.error(
          "HTTP request failed, method: {}, uri: {}, error: {}",
          method.name(),
          fullUri,
          ex.getMessage());
      final String failUri = request != null ? request.getURI().toASCIIString() : url;
      throw new RestRequestException(
          String.format("HTTP request failed: %s %s", method.name(), failUri), ex);
    }
  }

  private List<JettyBodyStrategy> loadRequestBodyStrategies(ClassScanner scanner) {
    LOG.info("Loading JettyBodyStrategies using scanner: {}", scanner.getClass().getSimpleName());
    final List<JettyBodyStrategy> strategies = new ArrayList<>();
    final Set<Class<? extends JettyBodyStrategy>> strategyClasses =
        scanner.getInstantiableSubtypesOf(JettyBodyStrategy.class);
    try {
      for (final Class<? extends JettyBodyStrategy> clazz : strategyClasses) {
        strategies.add(clazz.getDeclaredConstructor().newInstance());
        LOG.debug("Discovered and initialized strategy: {}", clazz.getName());
      }
    } catch (Exception ex) {
      throw new CriticalBootstrapException("Failed to auto-discover JettyBodyStrategy", ex);
    }
    LOG.info("Loaded {} request body strategy(ies)", strategies.size());
    return strategies;
  }

  private Map<String, List<String>> extractHeaders(Response jettyResponse) {
    final Map<String, List<String>> map = new HashMap<>();
    for (final HttpField field : jettyResponse.getHeaders()) {
      map.computeIfAbsent(field.getName(), k -> new ArrayList<>()).add(field.getValue());
    }
    return map;
  }
}
