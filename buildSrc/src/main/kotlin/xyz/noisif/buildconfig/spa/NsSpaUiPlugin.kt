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
package xyz.noisif.buildconfig.spa

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.yarn.task.YarnTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import xyz.noisif.buildconfig.AddDependencyAction
import xyz.noisif.buildconfig.TaskConfigureAction
import xyz.noisif.buildconfig.WithPluginAction
import xyz.noisif.buildconfig.alias.PluginAlias
import xyz.noisif.buildconfig.alias.apply
import xyz.noisif.buildconfig.alias.getPluginId

class NsSpaUiPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(target, PluginAlias.BASE)
    target.pluginManager.apply(target, PluginAlias.NODE)

    val extension = target.extensions.create("nsService", NsSpaUiExtension::class.java)
    target.extensions.getByType<NodeExtension>().apply {
      version.set(extension.nodeVersion)
      yarnVersion.set(extension.yarnVersion)
      download.set(true)
    }
    val prettierCheck = target.tasks.register<YarnTask>("prettierCheck") {
      dependsOn("yarn")
      args.set(listOf("run", "format:check"))
      inputs.dir("src")
      inputs.file("package.json")
      outputs.upToDateWhen { true }
    }
    val prettierWrite = target.tasks.register<YarnTask>("prettierWrite") {
      dependsOn("yarn")
      args.set(listOf("run", "format:write"))
    }
    val buildFrontend = target.tasks.register<YarnTask>("buildFrontend") {
      dependsOn("yarn")
      args.set(listOf("run", "build"))
      inputs.dir("src")
      inputs.file("index.html")
      inputs.file("package.json")
      inputs.file("vite.config.ts")
      inputs.file("tsconfig.json")
      outputs.dir("dist")
    }
    target.tasks.register<YarnTask>("runDev") {
      group = "application"
      dependsOn("yarn")
      args.set(listOf("run", "dev"))
    }
    target.tasks.named(
      "build",
      TaskConfigureAction { task ->
        task.dependsOn(buildFrontend)
      },
    )
    target.tasks.named(
      "clean",
      TaskConfigureAction { task ->
        (task as? Delete)?.delete("dist", "node_modules", ".gradle")
      },
    )
    target.pluginManager.withPlugin(
      getPluginId(target, PluginAlias.SPOTLESS),
      WithPluginAction {
        target.tasks.named("spotlessApply", AddDependencyAction(prettierWrite))
        target.tasks.named("spotlessCheck", AddDependencyAction(prettierCheck))
      },
    )
  }
}
