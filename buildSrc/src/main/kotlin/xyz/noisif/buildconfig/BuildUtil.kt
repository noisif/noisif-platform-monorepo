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
package xyz.noisif.buildconfig

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

fun getPluginId(accessor: Provider<PluginDependency>): String = accessor.get().pluginId

fun getEnv(name: String, defValue: String = ""): String = System.getenv("NS_$name") ?: defValue

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

fun getExecutableOsDependentFileName(): String {
  val osName = System.getProperty("os.name").lowercase()
  val arch = System.getProperty("os.arch").lowercase()
  return when {
    "win" in osName -> "win-amd64.exe"
    "mac" in osName && ("aarch" in arch || "arm" in arch) -> "macos-arm64"
    "mac" in osName -> "macos-amd64"
    else -> "linux-amd64"
  }
}
