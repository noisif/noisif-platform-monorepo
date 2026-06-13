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
package xyz.noisif.nsl.http.resolver.body;

import org.jspecify.annotations.NonNull;

import xyz.noisif.nsl.http.exception.RequestTooLargeException;

import java.io.IOException;
import java.io.InputStream;

class LimitedInputStream extends InputStream {
  private final InputStream delegate;
  private final long limit;
  private long bytesRead = 0;

  LimitedInputStream(InputStream delegate, long limit) {
    this.delegate = delegate;
    this.limit = limit;
  }

  @Override
  public int read() throws IOException {
    int b = delegate.read();
    if (b != -1) {
      checkLimit(1);
    }
    return b;
  }

  @Override
  public int read(byte @NonNull [] b, int off, int len) throws IOException {
    int count = delegate.read(b, off, len);
    if (count > 0) {
      checkLimit(count);
    }
    return count;
  }

  private void checkLimit(int count) {
    bytesRead += count;
    if (bytesRead > limit) {
      throw new RequestTooLargeException(
          String.format("Actual bytes read so far: %d, max allowed: %d bytes", bytesRead, limit));
    }
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }
}
