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

import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.plugins.ide.idea.model.IdeaModel
import xyz.noisif.buildconfig.SourceSetConfigureAction
import xyz.noisif.buildconfig.TaskConfigureAction
import xyz.noisif.buildconfig.TaskNameStartsWithSpec
import xyz.noisif.buildconfig.WithPluginAction

class NsPolyglotJsConfigAction(private val extension: NsPolyglotJsExtension) : Action<Project> {
  override fun execute(project: Project) {
    registerTasks(project, extension)
    configureSourceSets(project)
  }

  private fun registerTasks(project: Project, extension: NsPolyglotJsExtension) {
    val npmInstall =
      project.tasks.register<NpmTask>("npmInstallDeps") {
        val deps = mutableListOf("install")
        deps.addAll(extension.npmDependencies.get())

        args.set(deps)
        inputs.file("src/main/js/package.json")
        outputs.dir("src/main/js/node_modules")
      }
    val bundleTask =
      project.tasks.register<NpxTask>("bundleJs") {
        dependsOn(npmInstall)

        val outputDir = project.layout.projectDirectory.dir("src/main/generated/js")

        inputs.dir("src/main/js").withPropertyName("sourceDir")
        inputs.dir("src/main/js/node_modules").withPropertyName("nodeModules")
        outputs.dir(outputDir).withPropertyName("outputDir")

        doFirst {
          val outDirFile = outputDir.asFile
          if (!outDirFile.exists()) {
            outDirFile.mkdirs()
          }
        }
        command.set("esbuild")

        val esbuildArgs = mutableListOf<String>()
        extension.entryPoints.get().forEach { (alias, srcPath) ->
          val absoluteSrc = project.file("src/main/js/$srcPath").absolutePath
          esbuildArgs.add("$alias=$absoluteSrc")
        }
        esbuildArgs.addAll(
          listOf(
            "--bundle",
            "--format=iife",
            "--minify",
            "--outdir=${outputDir.asFile.absolutePath}",
          ),
        )
        args.set(esbuildArgs)
      }

    project.tasks.named(
      "processResources",
      TaskConfigureAction { task ->
        task.dependsOn(bundleTask)
      },
    )

    project.pluginManager.withPlugin(
      "com.diffplug.spotless",
      WithPluginAction {
        project.tasks.matching(TaskNameStartsWithSpec("spotless")).configureEach(
          TaskConfigureAction { task ->
            task.dependsOn(npmInstall)
            task.dependsOn(bundleTask)
          },
        )
      },
    )
  }

  private fun configureSourceSets(project: Project) {
    val sourceSets = project.extensions.getByType<SourceSetContainer>()
    val generatedBaseDir = project.file("src/main/generated")

    sourceSets.named("main", SourceSetConfigureAction(generatedBaseDir))

    project.plugins.withId(
      "idea",
      Action {
        val idea = project.extensions.getByType<IdeaModel>()
        val ideaModule = idea.module

        ideaModule.excludeDirs.add(project.file("src/main/js/node_modules"))
        ideaModule.sourceDirs.add(project.file("src/main/js"))
        ideaModule.generatedSourceDirs.add(generatedBaseDir)
        ideaModule.excludeDirs.remove(generatedBaseDir)
      },
    )
  }
}
