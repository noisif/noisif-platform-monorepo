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
package xyz.noisif.buildconfig.binary

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RelativePath
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import xyz.noisif.buildconfig.CopySpecConfigureAction
import xyz.noisif.buildconfig.binary.unpack.DefaultTarUnpackStrategy
import xyz.noisif.buildconfig.binary.unpack.TarXzUnpackStrategy
import xyz.noisif.buildconfig.binary.unpack.ZipUnpackStrategy
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal class BinaryProcessor(
  private val execOps: ExecOperations,
  private val fsOps: FileSystemOperations,
  private val archOps: ArchiveOperations,
  private val logger: Logger,
) {
  private val unpackStrategies = listOf(
    TarXzUnpackStrategy,
    ZipUnpackStrategy,
    DefaultTarUnpackStrategy,
  )

  fun process(
    urlStr: String,
    resourceName: String,
    tmpDownloadDir: File,
    stagingDir: File,
    shouldUnpack: Boolean,
    includesSet: Set<String>,
    excludesSet: Set<String>,
    applyExecutablePermissions: Boolean,
  ) {
    val fileName = urlStr.substringAfterLast("/")
    val downloadedFile = File(tmpDownloadDir, fileName)

    if (!downloadedFile.exists()) {
      logger.lifecycle("Downloading [$resourceName] from $urlStr...")
      downloadedFile.parentFile.mkdirs()
      URI.create(urlStr).toURL().openStream().use { input ->
        Files.copy(input, downloadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
      }
    }
    if (shouldUnpack) {
      logger.lifecycle("Unpacking archive: $fileName (with smart flattening & filtering)...")
      val unpackTempDir = File(tmpDownloadDir, "$resourceName-unpack")
      unpackTempDir.deleteRecursively()
      unpackTempDir.mkdirs()

      val strategy = unpackStrategies.firstOrNull { it.canUnpack(fileName) }
        ?: throw GradleException("No unpack strategy found for file: $fileName")
      strategy.unpack(downloadedFile, unpackTempDir, execOps, fsOps, archOps)

      stagingDir.mkdirs()
      fsOps.copy(
        CopySpecConfigureAction {
          it.from(unpackTempDir)
          it.into(stagingDir)
          it.eachFile(FlattenFileAction(includesSet, excludesSet))
          it.includeEmptyDirs = false
          it.isCaseSensitive = true
        },
      )
      unpackTempDir.deleteRecursively()
    } else {
      logger.lifecycle("Copying raw binary: $resourceName...")
      downloadedFile.copyTo(File(stagingDir, resourceName), overwrite = true)
    }
    if (applyExecutablePermissions) {
      stagingDir.walkTopDown().filter { it.isFile }.forEach { it.setExecutable(true) }
    }
  }

  fun isArchive(fileName: String): Boolean =
    fileName.matches(Regex(".*\\.(zip|tar|tar\\.gz|tgz|tar\\.bz2|tar\\.xz)$"))

  internal class FlattenFileAction(
    private val includesSet: Set<String>,
    private val excludesSet: Set<String>,
  ) : Action<FileCopyDetails> {
    private val baseExcludes = listOf(
      "**/*.txt",
      "**/*.md",
      "**/*.html",
      "**/LICENSE*",
      "**/README*",
      "**/man/**",
      "**/doc/**",
      "**/share/**",
    )

    override fun execute(details: FileCopyDetails) {
      val relativePathStr = details.relativePath.pathString
      if (baseExcludes.any { matchAntPattern(relativePathStr, it) }) {
        details.exclude()
        return
      }
      if (excludesSet.isNotEmpty() && excludesSet.any { matchAntPattern(relativePathStr, it) }) {
        details.exclude()
        return
      }
      if (includesSet.isNotEmpty()) {
        val matchesAnyInclude = includesSet.any { pattern ->
          val fullPattern = if (pattern.contains("/")) pattern else "**/$pattern"
          matchAntPattern(relativePathStr, fullPattern) || matchAntPattern(details.name, pattern)
        }
        if (!matchesAnyInclude) {
          details.exclude()
          return
        }
      }
      details.relativePath = RelativePath(true, details.name)
    }

    private fun matchAntPattern(text: String, pattern: String): Boolean {
      val regexPattern = pattern
        .replace(".", "\\.")
        .replace("**/", "(?:.*/)?")
        .replace("*", "[^/]*")
        .replace("?", ".")
      return text.matches(Regex("^$regexPattern$", RegexOption.IGNORE_CASE))
    }
  }
}
