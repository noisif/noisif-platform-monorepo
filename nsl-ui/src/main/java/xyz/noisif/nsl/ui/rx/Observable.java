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

import java.util.function.Consumer;
import java.util.function.Function;

public interface Observable<T> {
  T get();

  Subscription subscribe(Consumer<T> listener);

  default Subscription subscribeImmediate(Consumer<T> listener) {
    listener.accept(get());
    return subscribe(listener);
  }

  default <R> Observable<R> map(Function<T, R> mapper) {
    final State<R> mapped = new State<>(mapper.apply(get()));
    this.subscribe(val -> mapped.set(mapper.apply(val)));
    return mapped;
  }
}
