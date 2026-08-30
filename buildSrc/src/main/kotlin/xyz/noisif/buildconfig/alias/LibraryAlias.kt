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
package xyz.noisif.buildconfig.alias

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.provider.Provider

internal enum class LibraryAlias(private val alias: String) : DependencyAlias {
  LOGBACK_CLASSIC("logback.classic"),
  PROTOBUF_COMPILER("protoc"),
  SCALA_LIBRARY("scala.library"),
  ;

  override fun getAlias() = alias

  override fun toString() = getAlias()
}

internal fun VersionCatalog.getLibrary(
  libraryAlias: LibraryAlias,
): Provider<MinimalExternalModuleDependency> = findLibrary(libraryAlias.getAlias()).orElseThrow {
  IllegalArgumentException("Library '${libraryAlias.getAlias()}' not found in TOML")
}
