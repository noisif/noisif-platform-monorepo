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
package xyz.noisif.buildconfig.spotless

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import xyz.noisif.buildconfig.HostPlatform
import xyz.noisif.buildconfig.spotless.spec.ProtobufFormatSpec
import java.io.File

class NsSpotlessProtobufPlugin : NsSpotlessBasePlugin() {
  private val binariesDirectory = "gradle/tools/clang-format"

  private val hostPlatform = HostPlatform.current()
  private val spotlessProtobufBinariesPlatform = SpotlessProtobufBinariesPlatform.current()

  override fun SpotlessExtension.configureSpotless(
    root: Project,
    target: Project,
    licenseFile: File,
  ) {
    val clangBin = getClangFormatExecutable(target)
    val detectedVersion = getClangFormatVersion(clangBin, target)
    val formatSpec = ProtobufFormatSpec(root, licenseFile, detectedVersion, clangBin)
    formatSpec.applyFormat(this)
  }

  private fun getClangFormatExecutable(target: Project): File {
    val clangExe =
      target.rootProject.file("$binariesDirectory/${spotlessProtobufBinariesPlatform.platformKey}")
    check(clangExe.exists()) {
      "clang-format binary not found at: ${clangExe.absolutePath}"
    }
    if (!hostPlatform.isWindows && !clangExe.canExecute()) {
      clangExe.setExecutable(true)
    }
    return clangExe
  }

  private fun getClangFormatVersion(clangBin: File, target: Project): String {
    val process = ProcessBuilder(clangBin.absolutePath, "--version").start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    val detectedVersion = output.substringAfter("version ").substringBefore(" ").trim()
    target.logger.lifecycle("using local clang-format ($detectedVersion) from ${clangBin.path}")
    return detectedVersion
  }
}
