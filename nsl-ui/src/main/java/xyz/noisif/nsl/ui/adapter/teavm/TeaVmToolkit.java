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

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import xyz.noisif.nsl.ui.component.text.HeadingLevel;
import xyz.noisif.nsl.ui.spi.AlertBannerPeer;
import xyz.noisif.nsl.ui.spi.RouterPeer;
import xyz.noisif.nsl.ui.spi.UiToolkit;
import xyz.noisif.nsl.ui.spi.container.ContainerPeer;
import xyz.noisif.nsl.ui.spi.container.FlexContainerPeer;
import xyz.noisif.nsl.ui.spi.container.FormPeer;
import xyz.noisif.nsl.ui.spi.container.FramePeer;
import xyz.noisif.nsl.ui.spi.control.ButtonPeer;
import xyz.noisif.nsl.ui.spi.control.TextFieldPeer;
import xyz.noisif.nsl.ui.spi.http.HttpClientPeer;
import xyz.noisif.nsl.ui.spi.json.JsonPeer;
import xyz.noisif.nsl.ui.spi.text.HeadingPeer;
import xyz.noisif.nsl.ui.spi.text.TextPeer;

public final class TeaVmToolkit implements UiToolkit {
  private final HTMLDocument document = Window.current().getDocument();

  public ContainerPeer resolveAppMount(String id) {
    final HTMLElement htmlElement = document.getElementById(id);
    if (htmlElement == null) {
      throw new IllegalStateException("Mount element #" + id + " not found in index.html!");
    }
    return new TeaVmContainerPeer(htmlElement);
  }

  @Override
  public FramePeer createFrame() {
    return new TeaVmFramePeer(document);
  }

  @Override
  public ContainerPeer createContainer() {
    return new TeaVmContainerPeer(document.createElement("div"));
  }

  @Override
  public FlexContainerPeer createVBox() {
    return new TeaVmFlexPeer(document.createElement("div"), "ns-vbox");
  }

  @Override
  public FlexContainerPeer createHBox() {
    return new TeaVmFlexPeer(document.createElement("div"), "ns-hbox");
  }

  @Override
  public FormPeer createForm() {
    return new TeaVmFormPeer(document.createElement("form"));
  }

  @Override
  public HeadingPeer createHeading(HeadingLevel level) {
    return new TeaVmHeadingPeer(document.createElement(level.getHtmlTag()));
  }

  @Override
  public ButtonPeer createButton() {
    return new TeaVmButtonPeer(document.createElement("button"));
  }

  @Override
  public TextFieldPeer createTextField() {
    return new TeaVmTextFieldPeer(document);
  }

  @Override
  public AlertBannerPeer createAlertBanner() {
    return new TeaVmAlertPeer(document);
  }

  @Override
  public TextPeer createText() {
    return new TeaVmTextPeer(document.createElement("p"));
  }

  @Override
  public RouterPeer createRouter() {
    return new TeaVmHistoryRouterPeer();
  }

  @Override
  public HttpClientPeer createHttpClient() {
    return new TeaVmHttpClientPeer();
  }

  @Override
  public JsonPeer createJsonObject() {
    return new TeaVmJsonPeer();
  }

  @Override
  public JsonPeer parseJson(String rawJson) {
    return new TeaVmJsonPeer(rawJson);
  }
}
