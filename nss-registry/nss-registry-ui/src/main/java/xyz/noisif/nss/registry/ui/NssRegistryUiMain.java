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
package xyz.noisif.nss.registry.ui;

import xyz.noisif.nsl.ui.SpaEntrypoint;
import xyz.noisif.nsl.ui.auth.AuthSession;
import xyz.noisif.nsl.ui.http.HttpRequest;
import xyz.noisif.nsl.ui.http.RestClient;
import xyz.noisif.nsl.ui.router.Router;
import xyz.noisif.nss.registry.ui.view.DashboardView;
import xyz.noisif.nss.registry.ui.view.LoginView;
import xyz.noisif.nss.registry.ui.view.NotFoundView;

public class NssRegistryUiMain extends SpaEntrypoint {
  public static final RestClient API_V1 =
    new RestClient("/api/v1")
      .onUnauthorized(
        () -> {
          AuthSession.setAuthenticated(false);
          router().navigate("/login");
        });

  public static void main(String[] args) {
    new NssRegistryUiMain().launch();
  }

  @Override
  protected void verifyInitialSession(Runnable proceedToRouter) {
    API_V1.execute(
      HttpRequest.get("/auth/session"),
      response -> handleSessionState(response.isSuccess(), proceedToRouter),
      errorResponse -> handleSessionState(false, proceedToRouter),
      networkError -> handleSessionState(false, proceedToRouter));
  }

  private void handleSessionState(boolean active, Runnable proceedToRouter) {
    AuthSession.setAuthenticated(active);
    proceedToRouter.run();
  }

  @Override
  protected void configureRouter(Router.Builder router) {
    router
      .protectedRoute("/", DashboardView::new)
      .guestRoute("/login", LoginView::new)
      .notFound(NotFoundView::new);
  }
}
