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
package xyz.noisif.nsl.ui.component.control;

import xyz.noisif.nsl.ui.component.NComponent;
import xyz.noisif.nsl.ui.core.UiContext;
import xyz.noisif.nsl.ui.rx.Observable;
import xyz.noisif.nsl.ui.spi.control.ButtonPeer;

public class NButton extends NComponent {
  public NButton(String text, Runnable onClick) {
    super(UiContext.toolkit().createButton());
    final ButtonPeer buttonPeer = (ButtonPeer) peer;
    buttonPeer.setType(ButtonType.BUTTON);
    buttonPeer.setText(text);
    buttonPeer.setOnClick(onClick);
  }

  public NButton(String text) {
    super(UiContext.toolkit().createButton());
    final ButtonPeer buttonPeer = (ButtonPeer) peer;
    buttonPeer.setType(ButtonType.SUBMIT);
    buttonPeer.setText(text);
  }

  public void bindEnabled(Observable<Boolean> condition) {
    condition.subscribeImmediate(this::setEnabled);
  }

  public void bindLoading(Observable<Boolean> loadingCondition) {
    loadingCondition.subscribeImmediate(this::setLoading);
  }

  public void setText(String text) {
    ((ButtonPeer) peer).setText(text);
  }

  public void setEnabled(boolean enabled) {
    ((ButtonPeer) peer).setEnabled(enabled);
  }

  public void setLoading(boolean loading) {
    ((ButtonPeer) peer).setLoading(loading);
  }
}
