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
package xyz.noisif.nsl.ui.adapter.teavm;

import org.teavm.jso.JSBody;
import org.teavm.jso.ajax.XMLHttpRequest;

import xyz.noisif.nsl.ui.http.HttpRequest;
import xyz.noisif.nsl.ui.http.HttpResponse;
import xyz.noisif.nsl.ui.spi.http.HttpClientPeer;

import java.util.Map;
import java.util.function.Consumer;

final class TeaVmHttpClientPeer implements HttpClientPeer {
  @Override
  public void send(
      HttpRequest request, Consumer<HttpResponse> onSuccess, Consumer<String> onError) {
    final XMLHttpRequest xhr = new XMLHttpRequest();

    xhr.open(request.method().name(), request.url(), true);
    setTimeout(xhr, request.timeoutMs());

    setWithCredentials(xhr, request.includeCredentials());
    for (final Map.Entry<String, String> header : request.headers().entrySet()) {
      xhr.setRequestHeader(header.getKey(), header.getValue());
    }
    xhr.onComplete(
        () -> {
          final int status = xhr.getStatus();
          if (status == 0) {
            onError.accept("Network error: unable to reach the server");
          } else {
            onSuccess.accept(new HttpResponse(status, xhr.getResponseText()));
          }
        });
    xhr.onTimeout(
        evt -> {
          onError.accept("Request timeout: server took too long to respond");
        });
    if (request.body() != null) {
      xhr.send(request.body().toJson());
    } else {
      xhr.send();
    }
  }

  @JSBody(
      params = {"xhr", "withCredentials"},
      script = "xhr.withCredentials = withCredentials;")
  private static native void setWithCredentials(XMLHttpRequest xhr, boolean withCredentials);

  @JSBody(
      params = {"xhr", "timeoutMs"},
      script = "xhr.timeout = timeoutMs;")
  private static native void setTimeout(XMLHttpRequest xhr, int timeoutMs);
}
