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
package xyz.noisif.buildconfig.spotless.spec

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Action
import org.gradle.api.Project
import java.io.File

abstract class SpotlessFormatSpec<T : Any>(
  protected val root: Project,
  protected val licenseFile: File,
) : Action<T> {
  protected fun buildLicense(
    licenseFile: File,
    startToken: String,
    linePrefix: String,
    endToken: String,
  ): String {
    if (!licenseFile.exists()) {
      return ""
    }
    val rawText = licenseFile.readText().trim()
    return "$startToken\n" +
      rawText.lines().joinToString("\n") { "$linePrefix$it".trimEnd() } +
      "\n$endToken"
  }

  abstract override fun execute(spec: T)

  abstract fun applyFormat(spotless: SpotlessExtension)
}
