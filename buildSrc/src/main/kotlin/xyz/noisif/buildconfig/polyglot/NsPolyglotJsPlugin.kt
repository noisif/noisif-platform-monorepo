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
package xyz.noisif.buildconfig.polyglot

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import xyz.noisif.buildconfig.alias.PluginAlias
import xyz.noisif.buildconfig.alias.apply

class NsPolyglotJsPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.apply<NodePlugin>()
    target.pluginManager.apply(target, PluginAlias.IDEA)

    val extension = target.extensions.create("polyglotJs", NsPolyglotJsExtension::class.java)
    val nodeExtension = target.extensions.getByType(NodeExtension::class.java)

    nodeExtension.version.set("20.11.0")
    nodeExtension.download.set(true)
    nodeExtension.nodeProjectDir.set(target.file("src/main/js"))

    target.afterEvaluate(NsPolyglotJsConfigAction(extension))
  }
}
