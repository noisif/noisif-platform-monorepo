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

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.TypescriptExtension
import org.gradle.api.Project
import java.io.File

internal class TypescriptFormatSpec(root: Project, licenseFile: File) :
  UiSpotlessFormatSpec<TypescriptExtension>(root, licenseFile) {
  override fun execute(spec: TypescriptExtension) {
    spec.target("src/**/*.ts", "vite.config.ts")
    spec.targetExclude(*excludes)
    spec.licenseHeader(
      buildLicense(licenseFile, "/*", " * ", " */"),
      "(import|export|class|interface|type|enum|const|let|var|function|namespace|declare|///)",
    )
  }

  override fun applyFormat(spotless: SpotlessExtension) = spotless.typescript(this)
}
