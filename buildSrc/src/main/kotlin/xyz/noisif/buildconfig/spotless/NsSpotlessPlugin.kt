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
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
import org.gradle.api.Project
import java.io.File

class NsSpotlessPlugin : NsSpotlessBasePlugin() {
  override fun SpotlessExtension.configureSpotless(
    root: Project,
    target: Project,
    licenseFile: File,
  ) {
    java {
      target("src/**/*.java")
      targetExclude("build/generated/**/*.java")
      googleJavaFormat()
      replaceRegex(
        "Force enum semicolon to new line with indent",
        "(?m)^([ \\t]*)(.*?)(,\\s*;)",
        "$1$2,\n$1;",
      )
      importOrder("\\#", "com", "org", "xyz.noisif", "", "jakarta", "java", "javax")
      licenseHeader(buildLicense(licenseFile, "/*", " * ", " */"))
      trimTrailingWhitespace()
      endWithNewline()
    }
    kotlin {
      target("src/**/*.kt")
      licenseHeader(buildLicense(licenseFile, "/*", " * ", " */"))
      ktlint().editorConfigOverride(mapOf("indent_size" to "2"))
      trimTrailingWhitespace()
      endWithNewline()
    }
    kotlinGradle {
      target("**/*.gradle.kts")
      targetExclude("buildSrc/**/*.gradle.kts", "build/**/*.gradle.kts")
      licenseHeader(buildLicense(licenseFile, "/*", " * ", " */"), """(?m)^\s*[a-zA-Z@_]""")
      ktlint().editorConfigOverride(mapOf("indent_size" to "2"))
      trimTrailingWhitespace()
      endWithNewline()
    }
    format("xml") {
      target("src/**/*.xml")
      eclipseWtp(EclipseWtpFormatterStep.XML).configFile(root.file("spotless/wtp-xml.prefs"))
      licenseHeader(buildLicense(licenseFile, "<!-- ", "  ~ ", "  -->"), "^(<[^!?])")
      trimTrailingWhitespace()
      endWithNewline()
    }
    format("properties") {
      target("*.properties", "src/**/*.properties")
      targetExclude("build/**/*.properties")
      licenseHeader(buildLicense(licenseFile, "#", "# ", "#"), """[a-zA-Z]""")
      trimTrailingWhitespace()
      endWithNewline()
    }
  }
}
