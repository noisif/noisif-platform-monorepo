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
package xyz.noisif.nsl.ui.component;

import xyz.noisif.nsl.ui.rx.Observable;
import xyz.noisif.nsl.ui.spi.ComponentPeer;

public abstract class NComponent {
  protected final ComponentPeer peer;
  private boolean initialized = false;

  protected NComponent(ComponentPeer peer) {
    this.peer = peer;
  }

  public final void bindVisible(Observable<Boolean> condition) {
    condition.subscribeImmediate(this::setVisible);
  }

  public final void setId(String id) {
    peer.setId(id);
  }

  public final void ensureInitialized() {
    if (!initialized) {
      initialized = true;
      onInitialize();
    }
  }

  protected void onInitialize() {}

  public void setVisible(boolean visible) {
    peer.setVisible(visible);
  }

  public void addStyleClass(String cssClass) {
    peer.addStyleClass(cssClass);
  }

  public ComponentPeer getPeer() {
    ensureInitialized();
    return peer;
  }
}
