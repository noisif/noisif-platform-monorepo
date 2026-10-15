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
package xyz.noisif.nsl.ui.rx;

import xyz.noisif.nsl.common.bootstrap.ForbiddenInstantiationException;

public final class Bindings {
  private Bindings() {
    throw new ForbiddenInstantiationException(Bindings.class);
  }

  @SafeVarargs
  public static Observable<Boolean> allTrue(Observable<Boolean>... conditions) {
    final State<Boolean> combined = new State<>(evaluateAll(conditions));
    for (final Observable<Boolean> cond : conditions) {
      cond.subscribe(ignored -> combined.set(evaluateAll(conditions)));
    }
    return combined;
  }

  @SafeVarargs
  public static Observable<Boolean> allNotBlank(Observable<String>... textObservables) {
    final State<Boolean> combined = new State<>(evaluateNotBlank(textObservables));
    for (final Observable<String> textObs : textObservables) {
      textObs.subscribe(ignored -> combined.set(evaluateNotBlank(textObservables)));
    }
    return combined;
  }

  private static boolean evaluateAll(Observable<Boolean>[] conditions) {
    for (final Observable<Boolean> cond : conditions) {
      if (!Boolean.TRUE.equals(cond.get())) {
        return false;
      }
    }
    return true;
  }

  private static boolean evaluateNotBlank(Observable<String>[] texts) {
    for (final Observable<String> text : texts) {
      final String val = text.get();
      if (val == null || val.isBlank()) {
        return false;
      }
    }
    return true;
  }
}
