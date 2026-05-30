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

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.plugins.ide.idea.model.IdeaModel

class JwPolyglotJsPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.apply<NodePlugin>()
    target.pluginManager.apply("idea")

    val extension = target.extensions.create("polyglotJs", JwPolyglotJsExtension::class.java)
    val nodeExtension = target.extensions.getByType(NodeExtension::class.java)

    nodeExtension.version.set("20.11.0")
    nodeExtension.download.set(true)
    nodeExtension.nodeProjectDir.set(target.file("src/main/js"))

    target.afterEvaluate {
      registerTasks(project, extension)
      configureSourceSets(project)
    }
  }

  private fun registerTasks(project: Project, extension: JwPolyglotJsExtension) {
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
    project.tasks.named("processResources") {
      dependsOn(bundleTask)
    }
  }

  private fun configureSourceSets(project: Project) {
    val sourceSets = project.extensions.getByType<SourceSetContainer>()
    val generatedBaseDir = project.file("src/main/generated")
    sourceSets.named("main") {
      resources.srcDir(generatedBaseDir)
    }
    project.plugins.withId("idea") {
      val idea = project.extensions.getByType<IdeaModel>()
      with(idea.module) {
        excludeDirs.add(project.file("src/main/js/node_modules"))
        sourceDirs.add(project.file("src/main/js"))
        generatedSourceDirs.add(generatedBaseDir)
        excludeDirs.remove(generatedBaseDir)
      }
    }
  }
}
