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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.bootstrap.ForbiddenInstantiationException;
import xyz.noisif.nsl.common.util.math.MathUtil;

import java.util.concurrent.Callable;
import java.util.function.BiPredicate;

public class RetryExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(RetryExecutor.class);

  private RetryExecutor() {
    throw new ForbiddenInstantiationException(RetryExecutor.class);
  }

  public static <T, C> T executeSync(
      Callable<T> action,
      C context,
      RetryPolicyContext<C> policy,
      BiPredicate<Integer, T> retryableRes,
      BiPredicate<Integer, Exception> retryableErr)
      throws Exception {
    int attempt = 0;
    while (true) {
      attempt++;
      try {
        T result = action.call();
        if (shouldRetryResult(result, attempt, context, policy, retryableRes)) {
          performBackoff(attempt, policy);
          continue;
        }
        return result;
      } catch (Exception ex) {
        if (shouldRetryException(ex, attempt, context, policy, retryableErr)) {
          performBackoff(attempt, policy);
          continue;
        }
        throw ex;
      }
    }
  }

  private static <T, C> boolean shouldRetryResult(
      T result,
      int attempt,
      C context,
      RetryPolicyContext<C> policy,
      BiPredicate<Integer, T> retryableRes) {
    if (!retryableRes.test(attempt, result)) {
      return false;
    }
    if (policy.shouldRetry(attempt, context)) {
      LOG.debug("Result-based retry triggered, attempt: {}, context: {}", attempt, context);
      return true;
    }
    LOG.debug(
        "Result-based retry conditions met, but policy denied further attempts ({})", attempt);
    return false;
  }

  private static <C> boolean shouldRetryException(
      Exception ex,
      int attempt,
      C context,
      RetryPolicyContext<C> policy,
      BiPredicate<Integer, Exception> retryableErr) {
    if (!retryableErr.test(attempt, ex)) {
      return false;
    }
    if (policy.shouldRetry(attempt, context)) {
      LOG.info(
          "Exception-based retry triggered, attempt: {}, error: '{}', context: {}",
          attempt,
          ex.getMessage(),
          context);
      return true;
    }
    LOG.warn(
        "Retry policy exhausted or denied for exception: '{}' after {} attempts",
        ex.getMessage(),
        attempt);
    return false;
  }

  private static void performBackoff(int attempt, RetryPolicyContext<?> policy) {
    final long delay =
        MathUtil.calcExpBackoff(attempt, policy.getBackoffMs(), policy.getMaxBackoffMs());
    if (delay > 0) {
      LOG.trace("Performing backoff for attempt {}: {}ms", attempt, delay);
      try {
        Thread.sleep(delay);
      } catch (InterruptedException ex) {
        LOG.warn("Retry backoff interrupted", ex);
        Thread.currentThread().interrupt();
      }
    }
  }
}
