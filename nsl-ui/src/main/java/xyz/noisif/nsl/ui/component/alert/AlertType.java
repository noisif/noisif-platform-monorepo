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

import xyz.noisif.nsl.ui.core.CssClass;

public enum AlertType implements CssClass {
  INFO("ns-alert-info"),
  SUCCESS("ns-alert-success"),
  WARNING("ns-alert-warning"),
  DANGER("ns-alert-danger"),
  ;

  private final String cssClass;

  AlertType(String cssClass) {
    this.cssClass = cssClass;
  }

  @Override
  public String getCssClass() {
    return cssClass;
  }
}
