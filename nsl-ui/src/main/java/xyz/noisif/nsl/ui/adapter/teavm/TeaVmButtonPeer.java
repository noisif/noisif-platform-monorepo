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

import org.teavm.jso.dom.html.HTMLElement;

import xyz.noisif.nsl.ui.component.control.ButtonType;
import xyz.noisif.nsl.ui.spi.control.ButtonPeer;

final class TeaVmButtonPeer extends TeaVmComponentPeer implements ButtonPeer {
  TeaVmButtonPeer(HTMLElement element) {
    super(element);
    element.setClassName("ns-btn");
    element.setAttribute("type", ButtonType.BUTTON.getHtmlType());
  }

  @Override
  public void setText(String text) {
    element.setInnerText(text != null ? text : "");
  }

  @Override
  public void setType(ButtonType type) {
    element.setAttribute("type", type.getHtmlType());
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled) {
      element.removeAttribute("disabled");
    } else {
      element.setAttribute("disabled", "true");
    }
  }

  @Override
  public void setLoading(boolean loading) {
    if (loading) {
      element.getClassList().add("ns-loading");
    } else {
      element.getClassList().remove("ns-loading");
    }
  }

  @Override
  public void setOnClick(Runnable action) {
    element.addEventListener(
        "click",
        evt -> {
          if (action != null) {
            action.run();
          }
        });
  }
}
