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

import com.diffplug.gradle.spotless.JavaExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import java.io.File

class JavaFormatSpec(root: Project, licenseFile: File) :
  SpotlessFormatSpec<JavaExtension>(root, licenseFile) {
  override fun execute(spec: JavaExtension) {
    spec.target("src/**/*.java")
    spec.targetExclude("build/generated/**/*.java")
    spec.googleJavaFormat()
    spec.replaceRegex(
      "Force enum semicolon to new line with indent",
      "(?m)^([ \\t]*)(.*?)(,\\s*;)",
      "$1$2,\n$1;",
    )
    spec.importOrder("\\#", "com", "org", "xyz.noisif", "", "jakarta", "java", "javax")
    spec.licenseHeader(buildLicense(licenseFile, "/*", " * ", " */"))
    spec.trimTrailingWhitespace()
    spec.endWithNewline()
  }

  override fun applyFormat(spotless: SpotlessExtension) = spotless.java(this)
}
