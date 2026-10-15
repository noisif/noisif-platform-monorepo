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
package xyz.noisif.nsl.ui.fragment;

import xyz.noisif.nsl.ui.component.NDivider;
import xyz.noisif.nsl.ui.component.container.Justify;
import xyz.noisif.nsl.ui.component.container.NHBox;
import xyz.noisif.nsl.ui.component.container.NVBox;
import xyz.noisif.nsl.ui.component.container.Padding;
import xyz.noisif.nsl.ui.component.text.NParagraph;

import java.time.LocalDate;
import java.time.ZoneId;

public class NFooter extends NVBox {
  private final int currentYear = LocalDate.now(ZoneId.of("UTC")).getYear();

  @Override
  protected void initLayout() {
    add(new NDivider(), new NFooterContent());
  }

  private class NFooterContent extends NHBox {
    @Override
    protected void initLayout() {
      setPadding(Padding.MD);
      setJustify(Justify.SPACE_BETWEEN);
      add(
          new NParagraph("&copy; " + currentYear + " NOISIF. All rights reserved."),
          new NParagraph("Build version: 000000"));
    }
  }
}
