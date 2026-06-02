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

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.File

abstract class JwSpotlessBasePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply("com.diffplug.spotless")
    target.configure<SpotlessExtension> {
      val baseDir = target.rootProject.projectDir
      val licenseFile = File(baseDir, "spotless/license-header.txt")
      configureSpotless(target.rootProject, target, licenseFile)
    }
  }

  protected abstract fun SpotlessExtension.configureSpotless(
    root: Project,
    target: Project,
    licenseFile: File,
  )

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
}
