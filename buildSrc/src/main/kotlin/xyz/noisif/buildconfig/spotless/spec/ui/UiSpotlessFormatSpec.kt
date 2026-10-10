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
package xyz.noisif.buildconfig.spotless.spec.ui

import org.gradle.api.Project
import xyz.noisif.buildconfig.spotless.spec.SpotlessFormatSpec
import java.io.File

internal abstract class UiSpotlessFormatSpec<T : Any>(root: Project, licenseFile: File) :
  SpotlessFormatSpec<T>(root, licenseFile) {
  protected val excludes = arrayOf(
    "**/node_modules/**",
    "**/dist/**",
    "**/build/**",
    "**/.volumes/**",
    "**/.idea/**",
  )

  override fun isApplicable(target: Project): Boolean {
    val packageJsonFile = File(target.projectDir, "package.json")
    return packageJsonFile.exists()
  }
}
