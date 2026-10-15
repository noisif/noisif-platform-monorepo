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
package xyz.noisif.nsl.ui.component.alert;

import xyz.noisif.nsl.ui.component.NComponent;
import xyz.noisif.nsl.ui.core.UiContext;
import xyz.noisif.nsl.ui.spi.AlertBannerPeer;

public class NAlertBanner extends NComponent {
  public NAlertBanner(AlertType type, String message) {
    this(type, message, true);
  }

  public NAlertBanner(AlertType type) {
    this(type, "", false);
  }

  public NAlertBanner(AlertType type, String message, boolean visible) {
    super(UiContext.toolkit().createAlertBanner());
    final AlertBannerPeer ap = (AlertBannerPeer) peer;
    ap.setType(type);
    ap.setMessage(message);
    ap.setVisible(visible);
  }

  public void setMessage(String message) {
    ((AlertBannerPeer) peer).setMessage(message);
  }

  public void setType(AlertType type) {
    ((AlertBannerPeer) peer).setType(type);
  }

  public void showAlert(AlertType type, String message) {
    setType(type);
    setMessage(message);
    setVisible(true);
  }

  public void hide() {
    setVisible(false);
  }
}
