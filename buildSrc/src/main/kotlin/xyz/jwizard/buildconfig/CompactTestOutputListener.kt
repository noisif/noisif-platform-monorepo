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
