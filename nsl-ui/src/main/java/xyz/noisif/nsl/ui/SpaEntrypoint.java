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
package xyz.noisif.nsl.ui;

import xyz.noisif.nsl.ui.adapter.teavm.TeaVmToolkit;
import xyz.noisif.nsl.ui.core.UiContext;
import xyz.noisif.nsl.ui.router.Router;
import xyz.noisif.nsl.ui.spi.container.ContainerPeer;

public abstract class SpaEntrypoint {
  private static final String DEFAULT_MOUNT_ID = "app-mount";
  private static Router globalRouter;

  protected final void launch() {
    final TeaVmToolkit toolkit = new TeaVmToolkit();
    UiContext.setToolkit(toolkit);

    final ContainerPeer rootMount = toolkit.resolveAppMount(DEFAULT_MOUNT_ID);
    verifyInitialSession(
        () -> {
          Router.Builder builder = new Router.Builder(rootMount);
          configureRouter(builder);
          globalRouter = builder.buildAndStart();
        });
  }

  protected void verifyInitialSession(Runnable proceedToRouter) {
    proceedToRouter.run();
  }

  public static Router router() {
    if (globalRouter == null) {
      throw new IllegalStateException("Router has not been started yet");
    }
    return globalRouter;
  }

  protected abstract void configureRouter(Router.Builder router);
}
