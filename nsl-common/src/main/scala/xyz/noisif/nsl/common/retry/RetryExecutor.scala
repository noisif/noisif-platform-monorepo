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
package xyz.noisif.nsl.common.retry

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import xyz.noisif.nsl.common.util.math.MathUtil

import java.util.concurrent.Callable
import java.util.function.BiPredicate

import scala.annotation.tailrec
import scala.util.control.NonFatal

object RetryExecutor {
  private val log: Logger = LoggerFactory.getLogger(RetryExecutor.getClass)

  @throws[Exception]
  def executeSync[T, C](
      action: Callable[T],
      context: C,
      policy: RetryPolicyContext[C],
      retryableRes: BiPredicate[Integer, T],
      retryableErr: BiPredicate[Integer, Exception]
  ): T = {
    executeSyncScala[T, C](
      () => action.call(),
      context,
      policy,
      (attempt, res) => retryableRes.test(attempt, res),
      (attempt, err) => retryableErr.test(attempt, err)
    )
  }

  @throws[Exception]
  def executeSyncScala[T, C](
      action: () => T,
      context: C,
      policy: RetryPolicyContext[C],
      retryableRes: (Int, T) => Boolean,
      retryableErr: (Int, Exception) => Boolean
  ): T = {
    @tailrec
    def loop(attempt: Int): T = {
      val currentAttempt = attempt + 1
      val outcome: Either[Exception, T] = {
        try {
          Right(action())
        } catch {
          case NonFatal(ex: Exception) => Left(ex)
        }
      }
      outcome match {
        case Right(result) =>
          if (shouldRetryResult(result, currentAttempt, context, policy, retryableRes)) {
            performBackoff(currentAttempt, policy)
            loop(currentAttempt)
          } else {
            result
          }
        case Left(ex) =>
          if (shouldRetryException(ex, currentAttempt, context, policy, retryableErr)) {
            performBackoff(currentAttempt, policy)
            loop(currentAttempt)
          } else {
            throw ex
          }
      }
    }
    loop(0)
  }

  private def shouldRetryResult[T, C](
      result: T,
      attempt: Int,
      context: C,
      policy: RetryPolicyContext[C],
      retryableRes: (Int, T) => Boolean
  ): Boolean = {
    if (!retryableRes(attempt, result)) {
      false
    } else if (policy.shouldRetry(attempt, context)) {
      log.debug("Result-based retry triggered, attempt: {}, context: {}", attempt, context)
      true
    } else {
      log.debug(
        "Result-based retry conditions met, but policy denied further attempts ({})",
        attempt
      )
      false
    }
  }

  private def shouldRetryException[C](
      ex: Exception,
      attempt: Int,
      context: C,
      policy: RetryPolicyContext[C],
      retryableErr: (Int, Exception) => Boolean
  ): Boolean = {
    if (!retryableErr(attempt, ex)) {
      false
    } else if (policy.shouldRetry(attempt, context)) {
      log.info(
        "Exception-based retry triggered, attempt: {}, error: '{}', context: {}",
        attempt,
        ex.getMessage,
        context
      )
      true
    } else {
      log.warn(
        "Retry policy exhausted or denied for exception: '{}' after {} attempts",
        ex.getMessage,
        attempt
      )
      false
    }
  }

  private def performBackoff(attempt: Int, policy: RetryPolicyContext[?]): Unit = {
    val delay = MathUtil.calcExpBackoff(attempt, policy.backoffMs, policy.maxBackoffMs)
    if (delay > 0) {
      log.trace("Performing backoff for attempt {}: {}ms", attempt, delay)
      try {
        Thread.sleep(delay)
      } catch {
        case ex: InterruptedException =>
          log.warn("Retry backoff interrupted", ex)
          Thread.currentThread().interrupt()
      }
    }
  }
}
