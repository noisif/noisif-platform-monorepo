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

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.plugins.PluginManager
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency
import xyz.noisif.buildconfig.getPluginId
import xyz.noisif.buildconfig.libs

enum class PluginAlias(private val alias: String) : DependencyAlias {
  APPLICATION("application"),
  IDEA("idea"),
  PROTOBUF("protobuf"),
  SCALA("scala"),
  SHADOW("shadow"),
  ;

  override fun getAlias() = alias

  override fun toString() = getAlias()
}

fun VersionCatalog.getPlugin(pluginAlias: PluginAlias): Provider<PluginDependency> =
  findPlugin(pluginAlias.getAlias()).orElseThrow {
    IllegalArgumentException("Plugin '${pluginAlias.getAlias()}' not found in TOML")
  }

fun PluginManager.apply(target: Project, pluginAlias: PluginAlias) =
  apply(getPluginId(target.libs.getPlugin(pluginAlias)))
