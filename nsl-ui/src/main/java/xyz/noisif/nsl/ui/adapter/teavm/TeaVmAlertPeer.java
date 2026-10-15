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

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import xyz.noisif.nsl.ui.component.alert.AlertType;
import xyz.noisif.nsl.ui.spi.AlertBannerPeer;

final class TeaVmAlertPeer extends TeaVmComponentPeer implements AlertBannerPeer {
  private AlertType currentType = AlertType.INFO;
  private final HTMLElement messageSpan;

  TeaVmAlertPeer(HTMLDocument document) {
    super(document.createElement("div"));
    element.setClassName("ns-alert " + currentType.getCssClass() + " ns-alert-dismissible");

    messageSpan = document.createElement("span");
    element.appendChild(messageSpan);

    final HTMLElement closeBtn = document.createElement("button");
    closeBtn.setClassName("ns-alert-close");
    closeBtn.setAttribute("type", "button");
    closeBtn.addEventListener("click", evt -> setVisible(false));

    element.appendChild(closeBtn);
  }

  @Override
  public void setMessage(String message) {
    messageSpan.setInnerText(message != null ? message : "");
  }

  @Override
  public void setType(AlertType type) {
    swapClass(currentType.getCssClass(), type.getCssClass());
    currentType = type;
  }
}
