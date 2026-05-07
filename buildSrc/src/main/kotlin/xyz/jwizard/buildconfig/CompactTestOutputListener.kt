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

import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult

class CompactTestOutputListener(private val summaryService: TestSummaryService) : TestListener {
    override fun beforeSuite(suite: TestDescriptor) {
    }

    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
        if (suite.parent != null) {
            return
        }
        val passed = result.successfulTestCount
        val failed = result.failedTestCount
        val skipped = result.skippedTestCount
        val total = result.testCount

        summaryService.addResults(passed, failed, skipped)

        val plainPassedStr = "$passed PASSED"
        val plainFailedStr = "$failed FAILED"
        val plainSkippedStr = "$skipped SKIPPED"

        val summaryLine = "Test summary: $plainPassedStr, $plainFailedStr, $plainSkippedStr"
        val totalLine = "Total tests : $total"

        val maxLength = maxOf(summaryLine.length, totalLine.length)
        val separator = "=".repeat(maxLength)

        val passedStr = "$GREEN$plainPassedStr$RESET"
        val failedStr = "$RED$plainFailedStr$RESET"
        val skippedStr = "$YELLOW$plainSkippedStr$RESET"

        println("\n$separator")
        println("Test summary: $passedStr, $failedStr, $skippedStr")
        println("Total tests : $total")
        println("$separator\n")
    }

    override fun beforeTest(testDescriptor: TestDescriptor) {
    }

    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
        val name = testDescriptor.displayName
        val simpleClassName = testDescriptor.className?.substringAfterLast('.') ?: "UnknownClass"
        when (result.resultType) {
            TestResult.ResultType.SUCCESS ->
                println("$simpleClassName > $name ${GREEN}PASSED$RESET")

            TestResult.ResultType.FAILURE ->
                println("$simpleClassName > $name ${RED}FAILED$RESET")

            TestResult.ResultType.SKIPPED ->
                println("$simpleClassName > $name ${YELLOW}SKIPPED$RESET")

            else -> {}
        }
    }
}
