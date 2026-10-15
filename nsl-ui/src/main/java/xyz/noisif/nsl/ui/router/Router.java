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
package xyz.noisif.nsl.ui.router;

import xyz.noisif.nsl.ui.auth.AuthSession;
import xyz.noisif.nsl.ui.component.NComponent;
import xyz.noisif.nsl.ui.core.UiContext;
import xyz.noisif.nsl.ui.spi.RouterPeer;
import xyz.noisif.nsl.ui.spi.container.ContainerPeer;

import java.util.function.Supplier;

public final class Router {
  private final RouterPeer peer;

  private Router(RouterPeer peer) {
    this.peer = peer;
  }

  public void navigate(String path) {
    peer.navigate(path);
  }

  public static final class Builder {
    private final ContainerPeer mountPoint;
    private final RouterPeer routerPeer;
    private final Router routerInstance;
    private String loginRedirectPath = "/login";
    private String authenticatedRedirectPath = "/";

    public Builder(ContainerPeer mountPoint) {
      this.mountPoint = mountPoint;
      this.routerPeer = UiContext.toolkit().createRouter();
      this.routerInstance = new Router(routerPeer);
    }

    public Builder defaultRedirects(String loginPath, String homeAfterLoginPath) {
      this.loginRedirectPath = loginPath;
      this.authenticatedRedirectPath = homeAfterLoginPath;
      return this;
    }

    public Builder route(String path, Supplier<NComponent> viewFactory) {
      routerPeer.registerRoute(path, () -> renderView(viewFactory));
      return this;
    }

    public Builder guestRoute(String path, Supplier<NComponent> viewFactory) {
      routerPeer.registerRoute(
          path,
          () -> {
            if (AuthSession.isAuthenticated()) {
              routerPeer.navigate(authenticatedRedirectPath);
            } else {
              renderView(viewFactory);
            }
          });
      return this;
    }

    public Builder protectedRoute(String path, Supplier<NComponent> viewFactory) {
      routerPeer.registerRoute(
          path,
          () -> {
            if (AuthSession.isAuthenticated()) {
              renderView(viewFactory);
            } else {
              routerPeer.navigate(loginRedirectPath);
            }
          });
      return this;
    }

    public Builder notFound(Supplier<NComponent> viewFactory) {
      routerPeer.registerNotFound(() -> renderView(viewFactory));
      return this;
    }

    private void renderView(Supplier<NComponent> viewFactory) {
      mountPoint.clearChildren();
      final NComponent view = viewFactory.get();
      mountPoint.addChild(view.getPeer());
    }

    public Router buildAndStart() {
      routerPeer.start();
      return routerInstance;
    }
  }
}
