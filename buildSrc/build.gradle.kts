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
    implementation(libs.shadow.marker)
    implementation(libs.protoc)
    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        create("jwServicePlugin") {
            id = "xyz.jwizard.jw-service"
            implementationClass = "xyz.jwizard.buildconfig.JwServicePlugin"
        }
    }
    plugins {
        create("jwPolyglotJs") {
            id = "xyz.jwizard.jw-polyglot-js"
            implementationClass = "xyz.jwizard.buildconfig.JwPolyglotJsPlugin"
        }
    }
    plugins {
        create("jwProtobuf") {
            id = "xyz.jwizard.jw-protobuf"
            implementationClass = "xyz.jwizard.buildconfig.JwProtobufPlugin"
        }
    }
}

spotless {
    val baseDir = rootProject.projectDir.parentFile
    var rawLicenseFile = File(baseDir, "spotless/license-header.txt")
    kotlin {
        target("src/**/*.kt")
        licenseHeader(buildLicense(rawLicenseFile))
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        licenseHeader(buildLicense(rawLicenseFile), """(?m)^\s*[a-zA-Z@_]""")
        ktlint()
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
