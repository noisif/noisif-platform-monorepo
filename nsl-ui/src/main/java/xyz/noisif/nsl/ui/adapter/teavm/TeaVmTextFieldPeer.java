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
import org.teavm.jso.dom.html.HTMLInputElement;

import xyz.noisif.nsl.ui.spi.control.TextFieldPeer;

final class TeaVmTextFieldPeer extends TeaVmComponentPeer implements TextFieldPeer {
  private final HTMLInputElement input;
  private final HTMLElement errorLabel;

  TeaVmTextFieldPeer(HTMLDocument doc) {
    super(doc.createElement("div"));
    element.setClassName("ns-field-group");

    input = (HTMLInputElement) doc.createElement("input");
    input.setClassName("ns-input");
    input.setType("text");

    errorLabel = doc.createElement("span");
    errorLabel.setClassName("ns-error-msg ns-hidden");

    element.appendChild(input);
    element.appendChild(errorLabel);
  }

  @Override
  public void setId(String id) {
    if (id != null && !id.isBlank()) {
      input.setAttribute("id", id);
      input.setAttribute("name", id);
      element.setAttribute("id", id + "-group");
    }
  }

  @Override
  public String getValue() {
    return input.getValue() != null ? input.getValue() : "";
  }

  @Override
  public void setValue(String value) {
    input.setValue(value != null ? value : "");
  }

  @Override
  public void setPlaceholder(String placeholder) {
    input.setPlaceholder(placeholder != null ? placeholder : "");
  }

  @Override
  public void setPasswordMode(boolean isPassword) {
    input.setType(isPassword ? "password" : "text");
  }

  @Override
  public void setError(String errorMessage) {
    if (errorMessage == null || errorMessage.isBlank()) {
      input.getClassList().remove("ns-input-error");
      errorLabel.setInnerText("");
      errorLabel.getClassList().add("ns-hidden");
    } else {
      input.getClassList().add("ns-input-error");
      errorLabel.setInnerText(errorMessage);
      errorLabel.getClassList().remove("ns-hidden");
    }
  }

  @Override
  public void setOnChange(Runnable action) {
    input.addEventListener(
        "input",
        evt -> {
          if (action != null) {
            action.run();
          }
        });
  }
}
