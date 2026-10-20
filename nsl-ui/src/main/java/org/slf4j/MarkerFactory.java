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
package org.slf4j;

import xyz.noisif.nsl.ui.logger.NsSLF4JProvider;

public class MarkerFactory {
  private static final NsSLF4JProvider PROVIDER = new NsSLF4JProvider();

  static {
    PROVIDER.initialize();
  }

  private MarkerFactory() {}

  public static Marker getMarker(String name) {
    return PROVIDER.getMarkerFactory().getMarker(name);
  }

  public static Marker getDetachedMarker(String name) {
    return PROVIDER.getMarkerFactory().getDetachedMarker(name);
  }

  public static IMarkerFactory getIMarkerFactory() {
    return PROVIDER.getMarkerFactory();
  }
}
