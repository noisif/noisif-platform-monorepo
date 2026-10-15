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
package xyz.noisif.nss.registry.ui.view;

import xyz.noisif.nsl.ui.SpaEntrypoint;
import xyz.noisif.nsl.ui.component.alert.AlertType;
import xyz.noisif.nsl.ui.component.alert.NAlertBanner;
import xyz.noisif.nsl.ui.component.container.NVBox;
import xyz.noisif.nsl.ui.component.container.Padding;
import xyz.noisif.nsl.ui.component.container.Spacing;
import xyz.noisif.nsl.ui.component.control.NButton;

public class NotFoundView extends NVBox {
  @Override
  protected void initLayout() {
    setPadding(Padding.XL);
    setSpacing(Spacing.MD);
    add(
        new NAlertBanner(AlertType.WARNING, "404 - The requested page does not exist."),
        new NButton("Back to Home", () -> SpaEntrypoint.router().navigate("/")));
  }
}
