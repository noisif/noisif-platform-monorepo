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
package xyz.noisif.nsl.ui.spi;

import xyz.noisif.nsl.ui.component.text.HeadingLevel;
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

public interface UiToolkit {
  FramePeer createFrame();

  ContainerPeer createContainer();

  FlexContainerPeer createVBox();

  FlexContainerPeer createHBox();

  FormPeer createForm();

  HeadingPeer createHeading(HeadingLevel level);

  ButtonPeer createButton();

  TextFieldPeer createTextField();

  AlertBannerPeer createAlertBanner();

  TextPeer createText();

  RouterPeer createRouter();

  HttpClientPeer createHttpClient();

  JsonPeer createJsonObject();

  JsonPeer parseJson(String rawJson);
}
