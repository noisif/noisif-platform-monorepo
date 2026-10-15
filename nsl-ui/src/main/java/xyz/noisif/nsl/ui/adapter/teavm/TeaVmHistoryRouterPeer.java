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
import org.teavm.jso.browser.Window;

import xyz.noisif.nsl.ui.spi.RouterPeer;

import java.util.HashMap;
import java.util.Map;

final class TeaVmHistoryRouterPeer implements RouterPeer {
  private final Map<String, Runnable> routes = new HashMap<>();
  private Runnable notFoundHandler = () -> {};

  @Override
  public void registerRoute(String path, Runnable viewRenderer) {
    routes.put(path, viewRenderer);
  }

  @Override
  public void registerNotFound(Runnable notFoundRenderer) {
    this.notFoundHandler = notFoundRenderer;
  }

  @Override
  public void navigate(String path) {
    pushState(path);
    resolveCurrentRoute();
  }

  @Override
  public void start() {
    Window.current().addEventListener("popstate", evt -> resolveCurrentRoute());
    resolveCurrentRoute();
  }

  private void resolveCurrentRoute() {
    String currentPath = getPathname();
    Runnable handler = routes.getOrDefault(currentPath, notFoundHandler);
    handler.run();
  }

  @JSBody(
      params = {"path"},
      script = "window.history.pushState({}, '', path);")
  private static native void pushState(String path);

  @JSBody(script = "return window.location.pathname;")
  private static native String getPathname();
}
