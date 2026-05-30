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
package xyz.jwizard.buildconfig

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency
import java.io.File

fun getPluginId(accessor: Provider<PluginDependency>): String = accessor.get().pluginId

fun getEnv(name: String, defValue: String = ""): String = System.getenv("JW_$name") ?: defValue

val Project.libs: VersionCatalog
  get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

fun VersionCatalog.getPlugin(alias: String): Provider<PluginDependency> =
  findPlugin(alias).orElseThrow {
    IllegalArgumentException("Plugin '$alias' not found in TOML")
  }

fun VersionCatalog.getLibrary(alias: String): Provider<MinimalExternalModuleDependency> =
  findLibrary(alias).orElseThrow {
    IllegalArgumentException("Library '$alias' not found in TOML")
  }

fun buildLicense(
  licenseFile: File,
  startToken: String,
  linePrefix: String,
  endToken: String,
): String {
  val rawText = licenseFile.readText().trim()
  return "$startToken\n" +
    rawText.lines().joinToString("\n") { "$linePrefix$it".trimEnd() } +
    "\n$endToken"
}
