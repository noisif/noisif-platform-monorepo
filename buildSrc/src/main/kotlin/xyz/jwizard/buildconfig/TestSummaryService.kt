/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jwizard.buildconfig

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.atomic.AtomicLong

abstract class TestSummaryService : BuildService<BuildServiceParameters.None>, AutoCloseable {
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
        println("Total passed  : $GREEN$passed$RESET")
        println("Total failed  : $RED$failed$RESET")
        println("Total skipped : $YELLOW$skipped$RESET")
        println("Grand total   : $total")
        println("$separator\n")
    }
}

fun Project.registerTestSummaryService(): Provider<TestSummaryService> {
    return gradle.sharedServices.registerIfAbsent("testSummary", TestSummaryService::class.java) {
    }
}
