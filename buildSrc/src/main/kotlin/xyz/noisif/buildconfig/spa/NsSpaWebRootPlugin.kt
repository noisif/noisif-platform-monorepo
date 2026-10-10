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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import xyz.noisif.buildconfig.AddDependencyAction
import xyz.noisif.buildconfig.WithPluginAction
import xyz.noisif.buildconfig.alias.PluginAlias
import xyz.noisif.buildconfig.alias.getPluginId

class NsSpaWebRootPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val extension = target.extensions.create<NsSpaWebRootExtension>("nsSpaWebRoot")
    val targetDirProvider = extension.resourcesTargetDir.map { subDir ->
      val trimmed = subDir.trim()
      val path = if (trimmed.isEmpty()) "resources/main" else "resources/main/$trimmed"
      target.layout.buildDirectory.dir(path).get().asFile
    }
    val copyFrontendTask = target.tasks.register<Copy>("copyFrontendAssets") {
      val uiProjectProvider = extension.frontendProject.map { target.project(it) }
      from(uiProjectProvider.map { it.layout.projectDirectory.dir("dist") })
      into(targetDirProvider)
      doFirst {
        val destDir = targetDirProvider.get()
        if (!destDir.exists()) {
          destDir.mkdirs()
        }
      }
    }
    target.afterEvaluate {
      val uiProjectPath = extension.frontendProject.orNull
      if (uiProjectPath != null) {
        val uiProject = target.project(uiProjectPath)
        target.evaluationDependsOn(uiProjectPath)
        copyFrontendTask.configure(AddDependencyAction(uiProject.tasks.named("buildFrontend")))
      }
    }
    target.pluginManager.withPlugin(
      getPluginId(target, PluginAlias.JAVA),
      WithPluginAction {
        target.tasks.named("processResources", AddDependencyAction(copyFrontendTask))
      },
    )
    target.tasks.withType<ShadowJar>().configureEach(AddDependencyAction(copyFrontendTask))
  }
}
