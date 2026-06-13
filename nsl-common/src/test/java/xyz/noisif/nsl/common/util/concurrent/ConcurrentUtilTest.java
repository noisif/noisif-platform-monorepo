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
package xyz.noisif.nsl.common.util.concurrent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

class ConcurrentUtilTest {
  @Test
  @DisplayName("should complete successfully when onSuccess is invoked")
  void shouldCompleteSuccessfullyWhenOnSuccessInvoked() {
    // expect: no exception is thrown
    assertDoesNotThrow(() -> ConcurrentUtil.await(IoCallback::onSuccess));
  }

  @Test
  @DisplayName("should rethrow RuntimeException unwrapped")
  void shouldRethrowRuntimeExceptionUnwrapped() {
    // given
    final RuntimeException expectedException = new IllegalArgumentException("Invalid state");
    // when & then
    final IllegalArgumentException actualException =
        assertThrows(
            IllegalArgumentException.class,
            () -> ConcurrentUtil.await(callback -> callback.onFailure(expectedException)));
    assertEquals("Invalid state", actualException.getMessage());
  }

  @Test
  @DisplayName("should wrap checked exception in ConcurrentOperationException")
  void shouldWrapCheckedExceptionInConcurrentOperationException() {
    // given
    final Exception checkedException = new IOException("Disk failure");
    // when & then
    final ConcurrentOperationException actualException =
        assertThrows(
            ConcurrentOperationException.class,
            () -> ConcurrentUtil.await(callback -> callback.onFailure(checkedException)));
    assertEquals(checkedException, actualException.getCause());
  }

  @Test
  @Timeout(value = 1, unit = TimeUnit.SECONDS)
  @DisplayName("should block thread until callback is invoked")
  void shouldBlockThreadUntilCallbackIsInvoked() {
    // given
    final long startTime = System.currentTimeMillis();
    final long sleepTimeMs = 100;
    // when
    ConcurrentUtil.await(
        callback -> {
          // async I/O operation in separated thread
          Thread.ofVirtual()
              .start(
                  () -> {
                    try {
                      Thread.sleep(sleepTimeMs);
                      callback.onSuccess();
                    } catch (InterruptedException e) {
                      callback.onFailure(e);
                    }
                  });
        });
    // then
    final long executionTime = System.currentTimeMillis() - startTime;
    assertTrue(
        executionTime >= sleepTimeMs,
        "Method should block for at least " + sleepTimeMs + "ms, but took " + executionTime + "ms");
  }
}
