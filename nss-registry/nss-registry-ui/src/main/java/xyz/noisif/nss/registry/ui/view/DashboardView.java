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

import xyz.noisif.nsl.ui.component.alert.AlertType;
import xyz.noisif.nsl.ui.component.alert.NAlertBanner;
import xyz.noisif.nsl.ui.component.container.ContainerSize;
import xyz.noisif.nsl.ui.component.container.NConstraintBox;
import xyz.noisif.nsl.ui.component.container.NVBox;
import xyz.noisif.nsl.ui.component.container.Padding;
import xyz.noisif.nsl.ui.component.container.Spacing;
import xyz.noisif.nsl.ui.component.text.NParagraph;
import xyz.noisif.nsl.ui.fragment.NFooter;
import xyz.noisif.nsl.ui.fragment.NHeader;

public class DashboardView extends NConstraintBox {
  private final NAlertBanner alertBanner = new NAlertBanner(AlertType.SUCCESS);

  public DashboardView() {
    super();
    setSize(ContainerSize.XXL);
  }

  @Override
  protected void initLayout() {
    setPadding(Padding.MD);
    setSpacing(Spacing.MD);
    add(new NHeader("NOISIF Registry"), alertBanner, new DashboardContent(), new NFooter());
  }

  private class DashboardContent extends NVBox {
    @Override
    protected void initLayout() {
      setPadding(Padding.MD);
      add(new NParagraph("This is example content"));
      alertBanner.showAlert(AlertType.INFO, "You have been authenticated successfully.");
    }
  }
}
