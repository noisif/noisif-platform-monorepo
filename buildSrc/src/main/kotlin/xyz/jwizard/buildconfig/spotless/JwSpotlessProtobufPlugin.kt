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
package xyz.jwizard.buildconfig.spotless

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import xyz.jwizard.buildconfig.getExecutableOsDependentFileName
import java.io.File

class JwSpotlessProtobufPlugin : JwSpotlessBasePlugin() {
  private val binariesDirectory = "gradle/tools/clang-format"

  override fun SpotlessExtension.configureSpotless(
    root: Project,
    target: Project,
    licenseFile: File,
  ) {
    val clangExe = getClangFormatExecutable(target)
    val detectedVersion = getClangFormatVersion(clangExe, target)
    format("proto") {
      target("src/**/*.proto")
      targetExclude("build/**/*.proto")
      clangFormat(detectedVersion).pathToExe(clangExe.absolutePath)
      licenseHeader(
        buildLicense(licenseFile, "/*", " * ", " */"),
        """syntax = "proto3";""",
      )
      trimTrailingWhitespace()
      endWithNewline()
    }
  }

  private fun getClangFormatExecutable(target: Project): File {
    val fileName = getExecutableOsDependentFileName()
    val clangExe = target.rootProject.file("$binariesDirectory/$fileName")
    check(clangExe.exists()) {
      "Clang-format binary not found at: ${clangExe.absolutePath}"
    }
    if (!clangExe.canExecute()) {
      clangExe.setExecutable(true)
    }
    return clangExe
  }

  private fun getClangFormatVersion(clangExe: File, target: Project): String {
    val process = ProcessBuilder(clangExe.absolutePath, "--version").start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    val detectedVersion = output.substringAfter("version ").substringBefore(" ").trim()
    target.logger.lifecycle("Using local clang-format ($detectedVersion) from ${clangExe.path}")
    return detectedVersion
  }
}
