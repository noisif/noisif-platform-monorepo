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

import xyz.noisif.nsl.ui.component.NContainer;
import xyz.noisif.nsl.ui.spi.container.FlexContainerPeer;

public abstract class NFlexBox extends NContainer<FlexContainerPeer> {
  protected NFlexBox(FlexContainerPeer peer) {
    super(peer);
  }

  protected final void setSpacing(Spacing spacing) {
    typedPeer().setSpacing(spacing);
  }

  protected final void setPadding(Padding padding) {
    typedPeer().setPadding(padding);
  }

  protected final void setAlign(Align align) {
    typedPeer().setAlignment(align);
  }

  protected final void setJustify(Justify justify) {
    typedPeer().setJustification(justify);
  }
}
