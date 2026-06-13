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

plugins {
  alias(libs.plugins.idea)
  alias(libs.plugins.java.gradle.plugin)
  alias(libs.plugins.kotlin.dsl)
  alias(libs.plugins.protobuf) apply false
  alias(libs.plugins.shadow) apply false
  alias(libs.plugins.spotless)
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation(libs.gradle.node.plugin)
  implementation(libs.protobuf.gradle.plugin)
  implementation(libs.protoc)
  implementation(libs.shadow.marker)
  implementation(libs.spotless.gradle.plugin)
  implementation(gradleApi())
}

gradlePlugin {
  plugins {
    create("nsServicePlugin") {
      id = "xyz.noisif.ns-service"
      implementationClass = "xyz.noisif.buildconfig.service.NsServicePlugin"
    }
    create("nsPolyglotJs") {
      id = "xyz.noisif.ns-polyglot-js"
      implementationClass = "xyz.noisif.buildconfig.polyglot.NsPolyglotJsPlugin"
    }
    create("nsProtobuf") {
      id = "xyz.noisif.ns-protobuf"
      implementationClass = "xyz.noisif.buildconfig.NsProtobufPlugin"
    }
    create("nsSpotless") {
      id = "xyz.noisif.ns-spotless"
      implementationClass = "xyz.noisif.buildconfig.spotless.NsSpotlessPlugin"
    }
    create("nsSpotlessProtobuf") {
      id = "xyz.noisif.ns-spotless-protobuf"
      implementationClass = "xyz.noisif.buildconfig.spotless.NsSpotlessProtobufPlugin"
    }
  }
}

spotless {
  val baseDir = rootProject.projectDir.parentFile
  var rawLicenseFile = File(baseDir, "spotless/license-header.txt")
  kotlin {
    target("src/**/*.kt")
    licenseHeader(buildLicense(rawLicenseFile))
    ktlint().editorConfigOverride(mapOf("indent_size" to "2"))
    trimTrailingWhitespace()
    endWithNewline()
  }
  kotlinGradle {
    target("*.gradle.kts")
    licenseHeader(buildLicense(rawLicenseFile), """(?m)^\s*[a-zA-Z@_]""")
    ktlint().editorConfigOverride(mapOf("indent_size" to "2"))
    trimTrailingWhitespace()
    endWithNewline()
  }
}

idea {
  module {
    excludeDirs.add(file(".kotlin"))
  }
}

fun buildLicense(rawTextFile: File): String {
  val rawText = rawTextFile.readText().trim()
  return "/*\n" + rawText.lines().joinToString("\n") { " * $it".trimEnd() } + "\n */"
}
