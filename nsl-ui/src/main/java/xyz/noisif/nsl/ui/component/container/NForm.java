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
package xyz.noisif.nsl.ui.component.container;

import xyz.noisif.nsl.ui.component.NComponent;
import xyz.noisif.nsl.ui.component.NContainer;
import xyz.noisif.nsl.ui.core.UiContext;
import xyz.noisif.nsl.ui.spi.container.FormPeer;
import xyz.noisif.nsl.ui.validation.Validatable;

public abstract class NForm extends NFlexBox {
  protected NForm() {
    super(UiContext.toolkit().createForm());
    ((FormPeer) peer).setOnSubmit(this::handleFormSubmit);
  }

  protected NForm(Spacing spacing) {
    this();
    setSpacing(spacing);
  }

  private void handleFormSubmit() {
    if (validateAllChildren(this)) {
      onValidSubmit();
    } else {
      onValidationFailed();
    }
  }

  private boolean validateAllChildren(NContainer<?> container) {
    boolean allValid = true;
    for (final NComponent child : container.getChildren()) {
      if (child instanceof Validatable validatable) {
        allValid &= validatable.validate();
      }
      if (child instanceof NContainer<?> nestedContainer) {
        allValid &= validateAllChildren(nestedContainer);
      }
    }
    return allValid;
  }

  protected abstract void onValidSubmit();

  protected void onValidationFailed() {}
}
