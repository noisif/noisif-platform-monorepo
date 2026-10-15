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
package xyz.noisif.nsl.ui.auth;

import xyz.noisif.nsl.common.bootstrap.ForbiddenInstantiationException;
import xyz.noisif.nsl.ui.rx.Observable;
import xyz.noisif.nsl.ui.rx.State;

public final class AuthSession {
  private static final State<Boolean> authenticatedState = new State<>(false);

  private AuthSession() {
    throw new ForbiddenInstantiationException(AuthSession.class);
  }

  public static boolean isAuthenticated() {
    return Boolean.TRUE.equals(authenticatedState.get());
  }

  public static void setAuthenticated(boolean authenticated) {
    authenticatedState.set(authenticated);
  }

  public static Observable<Boolean> authenticatedProperty() {
    return authenticatedState;
  }
}
