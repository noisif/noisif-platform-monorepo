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
package xyz.noisif.buildconfig.ffmpeg.libav

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import xyz.noisif.buildconfig.CopySpecConfigureAction
import xyz.noisif.buildconfig.HostPlatform
import java.io.File
import java.net.URI

internal abstract class LibAvDownloader(
  protected val project: Project,
  protected val logger: Logger,
  protected val version: String,
  protected val targetDir: File,
) {
  protected abstract val toolName: String
  open val isEnabled: Boolean = true

  private val hostPlatform = HostPlatform.current()
  private val libAvBinariesPlatform = LibAvBinariesPlatform.current()

  companion object {
    private const val LIB_AV_URL_TEMPLATE =
      "https://github.com/ffbinaries/ffbinaries-prebuilt/releases/download/v%s/%s-%s-%s.zip"
  }

  fun download() {
    if (isBinaryAlreadyPresent()) {
      logger.lifecycle("$toolName (v$version) is already present at: ${targetDir.absolutePath}")
      return
    }

    val archiveFile = downloadArchive()
    extractArchive(archiveFile)
    applyExecutablePermissionsIfNeeded()
    verifyBinaryExecution()
    archiveFile.delete()
  }

  private fun isBinaryAlreadyPresent(): Boolean =
    File(targetDir, hostPlatform.formatBinaryName(toolName)).exists()

  private fun downloadArchive(): File {
    targetDir.mkdirs()
    val url = buildDownloadUrl()
    val archiveFile = project.layout.buildDirectory.file("$toolName-download.zip").get().asFile

    logger.lifecycle("downloading $toolName v$version for [${project.name}] from: $url")
    archiveFile.parentFile.mkdirs()

    URI.create(url).toURL().openStream().use { input ->
      archiveFile.outputStream().use { output -> input.copyTo(output) }
    }
    return archiveFile
  }

  private fun buildDownloadUrl(): String =
    LIB_AV_URL_TEMPLATE.format(version, toolName, version, libAvBinariesPlatform.platformKey)

  private fun extractArchive(archiveFile: File) {
    logger.lifecycle("extracting $toolName to ${targetDir.absolutePath}...")
    project.copy(
      CopySpecConfigureAction { spec ->
        spec.from(project.zipTree(archiveFile))
        spec.into(targetDir)
      },
    )
  }

  private fun applyExecutablePermissionsIfNeeded() {
    if (!hostPlatform.isWindows) {
      File(targetDir, toolName).setExecutable(true)
    }
  }

  private fun verifyBinaryExecution() {
    val binaryFile = File(targetDir, hostPlatform.formatBinaryName(toolName))
    try {
      val process = ProcessBuilder(binaryFile.absolutePath, "-version")
        .redirectErrorStream(true)
        .start()

      val output = process.inputStream.bufferedReader().use { it.readLine() } ?: ""
      val exitCode = process.waitFor()

      if (exitCode == 0) {
        logger.lifecycle("verified $toolName successfully: $output")
      } else {
        logger.warn("verification of $toolName returned non-zero exit code: $exitCode")
      }
    } catch (ex: Exception) {
      logger.error("failed to verify execution of $toolName: ${ex.message}")
    }
  }
}
