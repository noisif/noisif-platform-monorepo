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

import xyz.noisif.nsl.ui.spi.ComponentPeer;
import xyz.noisif.nsl.ui.spi.container.FramePeer;

final class TeaVmFramePeer extends TeaVmContainerPeer implements FramePeer {
  private final HTMLElement titleHeader;
  private final HTMLElement contentBody;

  TeaVmFramePeer(HTMLDocument doc) {
    super(doc.createElement("div"));
    element.setClassName("ns-frame");

    titleHeader = doc.createElement("div");
    titleHeader.setClassName("ns-frame-header");

    contentBody = doc.createElement("div");
    contentBody.setClassName("ns-frame-body");

    element.appendChild(titleHeader);
    element.appendChild(contentBody);
  }

  @Override
  public void setTitle(String title) {
    if (title == null || title.isBlank()) {
      titleHeader.setInnerText("");
      titleHeader.getClassList().add("ns-hidden");
    } else {
      titleHeader.setInnerText(title);
      titleHeader.getClassList().remove("ns-hidden");
    }
  }

  @Override
  public void addChild(ComponentPeer child) {
    if (child instanceof TeaVmComponentPeer teaChild) {
      contentBody.appendChild(teaChild.element);
    }
  }

  @Override
  public void clearChildren() {
    contentBody.setInnerHTML("");
  }
}
