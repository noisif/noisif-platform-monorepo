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
package xyz.noisif.buildconfig.binary.unpack

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.process.ExecOperations
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal object TarXzUnpackStrategy : ArchiveUnpackStrategy {
  override fun canUnpack(fileName: String) = fileName.endsWith(".tar.xz")

  override fun unpack(
    archiveFile: File,
    targetDir: File,
    execOps: ExecOperations,
    fsOps: FileSystemOperations,
    archOps: ArchiveOperations,
  ) {
    FileInputStream(archiveFile).use { fis ->
      BufferedInputStream(fis).use { bis ->
        XZCompressorInputStream(bis).use { xZis ->
          TarArchiveInputStream(xZis).use { tarIs ->
            var entry = tarIs.nextEntry
            while (entry != null) {
              val destPath = File(targetDir, entry.name).canonicalFile
              if (!destPath.path.startsWith(targetDir.canonicalPath)) {
                throw SecurityException("Attempted to escape target directory: ${entry.name}")
              }
              if (entry.isDirectory) {
                destPath.mkdirs()
              } else {
                destPath.parentFile?.mkdirs()
                Files.copy(tarIs, destPath.toPath(), StandardCopyOption.REPLACE_EXISTING)
              }
              entry = tarIs.nextEntry
            }
          }
        }
      }
    }
  }
}
