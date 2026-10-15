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
package xyz.noisif.nsl.ui.http;

import xyz.noisif.nsl.ui.core.UiContext;
import xyz.noisif.nsl.ui.rx.Observable;
import xyz.noisif.nsl.ui.rx.State;
import xyz.noisif.nsl.ui.spi.http.HttpClientPeer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class RestClient {
  private final String baseUrl;
  private final List<HttpRequestInterceptor> interceptors = new ArrayList<>();
  private Runnable unauthorizedHandler;

  private final State<Boolean> loadingState = new State<>(false);
  private final State<Boolean> idleState = new State<>(true);
  private int activeRequests = 0;

  public RestClient() {
    this("");
  }

  public RestClient(String baseUrl) {
    this.baseUrl = baseUrl != null ? baseUrl : "";
  }

  public Observable<Boolean> loadingProperty() {
    return loadingState;
  }

  public Observable<Boolean> idleProperty() {
    return idleState;
  }

  public RestClient addInterceptor(HttpRequestInterceptor interceptor) {
    this.interceptors.add(interceptor);
    return this;
  }

  public RestClient onUnauthorized(Runnable handler) {
    this.unauthorizedHandler = handler;
    return this;
  }

  public void execute(
      HttpRequest.Builder requestBuilder,
      Consumer<HttpResponse> onSuccess,
      Consumer<HttpResponse> onError,
      Consumer<String> onNetworkError) {
    for (final HttpRequestInterceptor interceptor : interceptors) {
      interceptor.intercept(requestBuilder);
    }
    final HttpRequest finalRequest = rebuildWithBaseUrl(requestBuilder.build());
    final HttpClientPeer peer = UiContext.toolkit().createHttpClient();

    incrementLoading();
    peer.send(
        finalRequest,
        response -> {
          decrementLoading();
          if (response.isSuccess()) {
            onSuccess.accept(response);
          } else {
            if (response.isUnauthorized() && unauthorizedHandler != null) {
              unauthorizedHandler.run();
            }
            onError.accept(response);
          }
        },
        networkErrorMsg -> {
          decrementLoading();
          onNetworkError.accept(networkErrorMsg);
        });
  }

  public void execute(
      HttpRequest.Builder requestBuilder,
      Consumer<HttpResponse> onSuccess,
      Consumer<HttpResponse> onError) {
    execute(requestBuilder, onSuccess, onError, err -> {});
  }

  private HttpRequest rebuildWithBaseUrl(HttpRequest req) {
    if (baseUrl.isEmpty() || req.url().startsWith("http")) {
      return req;
    }
    final HttpRequest.Builder copy =
        new HttpRequest.Builder(req.method(), baseUrl + req.url())
            .withCredentials(req.includeCredentials());
    req.headers().forEach(copy::header);
    if (req.body() != null) {
      copy.jsonBody(req.body());
    }
    return copy.build();
  }

  private void incrementLoading() {
    activeRequests++;
    loadingState.set(true);
    idleState.set(false);
  }

  private void decrementLoading() {
    activeRequests = Math.max(0, activeRequests - 1);
    if (activeRequests == 0) {
      loadingState.set(false);
      idleState.set(true);
    }
  }
}
