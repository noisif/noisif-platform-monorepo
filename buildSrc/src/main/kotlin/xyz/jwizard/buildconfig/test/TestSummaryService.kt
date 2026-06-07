/*
 * Copyright (c) 2022-2026 JWizard. All Rights Reserved.
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
package xyz.jwizard.buildconfig.test

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import xyz.jwizard.buildconfig.GREEN
import xyz.jwizard.buildconfig.RED
import xyz.jwizard.buildconfig.RESET
import xyz.jwizard.buildconfig.YELLOW
import java.util.concurrent.atomic.AtomicLong

abstract class TestSummaryService :
  BuildService<BuildServiceParameters.None>,
  AutoCloseable {
  private val totalPassed = AtomicLong(0)
  private val totalFailed = AtomicLong(0)
  private val totalSkipped = AtomicLong(0)

  fun addResults(passed: Long, failed: Long, skipped: Long) {
    totalPassed.addAndGet(passed)
    totalFailed.addAndGet(failed)
    totalSkipped.addAndGet(skipped)
  }

  override fun close() {
    val passed = totalPassed.get()
    val failed = totalFailed.get()
    val skipped = totalSkipped.get()
    val total = passed + failed + skipped

    if (total == 0L) {
      return
    }
    val header = "GLOBAL TEST SUMMARY (ALL MODULES)"
    val separator = "=".repeat(header.length)

    println("\n$separator")
    println(header)
    println("Total passed  : ${GREEN}$passed${RESET}")
    println("Total failed  : ${RED}$failed${RESET}")
    println("Total skipped : ${YELLOW}$skipped${RESET}")
    println("Grand total   : $total")
    println("$separator\n")
  }
}

fun Project.registerTestSummaryService(): Provider<TestSummaryService> =
  gradle.sharedServices.registerIfAbsent("testSummary", TestSummaryService::class.java) {
  }
