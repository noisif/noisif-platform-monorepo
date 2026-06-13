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
package xyz.noisif.nsl.common.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

@ExtendWith(MockitoExtension.class)
class RetryExecutorTest {
  private final String context = "test-context";
  @Mock private RetryPolicyContext<String> policy;

  @BeforeEach
  void setUp() {
    lenient().when(policy.getBackoffMs()).thenReturn(0L);
    lenient().when(policy.getMaxBackoffMs()).thenReturn(0L);
  }

  @Test
  @DisplayName("should return result immediately on first success")
  void shouldReturnImmediately() throws Exception {
    // given
    final Callable<String> action = () -> "success";
    // when
    final String result =
        RetryExecutor.executeSync(action, context, policy, (at, res) -> false, (at, ex) -> false);
    // then
    assertThat(result).isEqualTo("success");
    verify(policy, times(0)).shouldRetry(anyInt(), eq(context));
  }

  @Test
  @DisplayName("should retry when result matches retryable predicate and then succeed")
  void shouldRetryOnResult() throws Exception {
    // given
    final AtomicInteger attempts = new AtomicInteger(0);
    final Callable<Integer> action = attempts::incrementAndGet;
    final BiPredicate<Integer, Integer> retryOnOne = (attempt, res) -> res == 1;
    when(policy.shouldRetry(eq(1), eq(context))).thenReturn(true);
    // when
    final Integer result =
        RetryExecutor.executeSync(action, context, policy, retryOnOne, (at, ex) -> false);
    // then
    assertThat(result).isEqualTo(2);
    assertThat(attempts.get()).isEqualTo(2);
    verify(policy).shouldRetry(1, context);
  }

  @Test
  @DisplayName("should retry when exception occurs and then succeed")
  void shouldRetryOnException() throws Exception {
    // given
    final AtomicInteger attempts = new AtomicInteger(0);
    final Callable<String> action =
        () -> {
          if (attempts.incrementAndGet() == 1) {
            throw new RuntimeException("temporary error");
          }
          return "recovered";
        };
    when(policy.shouldRetry(eq(1), eq(context))).thenReturn(true);
    final BiPredicate<Integer, Exception> retryOnRuntime =
        (at, ex) -> ex instanceof RuntimeException;
    // when
    final String result =
        RetryExecutor.executeSync(action, context, policy, (at, res) -> false, retryOnRuntime);
    // then
    assertThat(result).isEqualTo("recovered");
    assertThat(attempts.get()).isEqualTo(2);
  }

  @Test
  @DisplayName("should throw exception when retry limit is reached")
  void shouldFailAfterExhaustingRetries() {
    // given
    final Callable<String> action =
        () -> {
          throw new RuntimeException("persistent error");
        };
    when(policy.shouldRetry(anyInt(), eq(context))).thenReturn(true);
    when(policy.shouldRetry(eq(3), eq(context))).thenReturn(false);
    final BiPredicate<Integer, Exception> retryAlways = (at, ex) -> true;
    // when & then
    assertThatThrownBy(
            () ->
                RetryExecutor.executeSync(action, context, policy, (at, res) -> false, retryAlways))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("persistent error");
    verify(policy, times(3)).shouldRetry(anyInt(), eq(context));
  }

  @Test
  @DisplayName("should respect backoff parameters from policy")
  void shouldRespectBackoff() throws Exception {
    // given
    final AtomicInteger attempts = new AtomicInteger(0);
    final Callable<String> action =
        () -> {
          if (attempts.incrementAndGet() == 1) return "retry-me";
          return "done";
        };
    when(policy.getBackoffMs()).thenReturn(10L);
    when(policy.getMaxBackoffMs()).thenReturn(100L);
    when(policy.shouldRetry(eq(1), eq(context))).thenReturn(true);
    // when
    RetryExecutor.executeSync(
        action, context, policy, (at, res) -> res.equals("retry-me"), (at, ex) -> false);
    // then
    verify(policy, atLeastOnce()).getBackoffMs();
    verify(policy, atLeastOnce()).getMaxBackoffMs();
  }
}
