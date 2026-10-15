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

import xyz.noisif.nsl.ui.component.container.Align;
import xyz.noisif.nsl.ui.component.container.Justify;
import xyz.noisif.nsl.ui.component.container.Padding;
import xyz.noisif.nsl.ui.component.container.Spacing;
import xyz.noisif.nsl.ui.spi.container.FlexContainerPeer;

class TeaVmFlexPeer extends TeaVmContainerPeer implements FlexContainerPeer {
  private Spacing currentSpacing;
  private Padding currentPadding;
  private Align currentAlign;
  private Justify currentJustify;

  TeaVmFlexPeer(HTMLElement element, String baseClass) {
    super(element);
    element.setClassName(baseClass);
  }

  @Override
  public void setSpacing(Spacing spacing) {
    swapClass(currentSpacing != null ? currentSpacing.getCssClass() : null, spacing.getCssClass());
    currentSpacing = spacing;
  }

  @Override
  public void setPadding(Padding padding) {
    swapClass(currentPadding != null ? currentPadding.getCssClass() : null, padding.getCssClass());
    currentPadding = padding;
  }

  @Override
  public void setAlignment(Align align) {
    swapClass(currentAlign != null ? currentAlign.getCssClass() : null, align.getCssClass());
    currentAlign = align;
  }

  @Override
  public void setJustification(Justify justify) {
    swapClass(currentJustify != null ? currentJustify.getCssClass() : null, justify.getCssClass());
    currentJustify = justify;
  }
}
