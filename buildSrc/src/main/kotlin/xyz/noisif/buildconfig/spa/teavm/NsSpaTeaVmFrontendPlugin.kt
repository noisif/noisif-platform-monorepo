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
package xyz.noisif.buildconfig.spa.teavm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.teavm.gradle.api.TeaVMExtension
import xyz.noisif.buildconfig.ArtifactViewConfigureAction
import xyz.noisif.buildconfig.AttributeContainerConfigureAction
import xyz.noisif.buildconfig.ComponentFilterSpec
import xyz.noisif.buildconfig.ConfigurationConfigureAction
import xyz.noisif.buildconfig.CopySpecConfigureAction
import xyz.noisif.buildconfig.GradleRunScript
import xyz.noisif.buildconfig.TaskConfigureAction
import xyz.noisif.buildconfig.alias.LibraryAlias
import xyz.noisif.buildconfig.alias.PluginAlias
import xyz.noisif.buildconfig.alias.apply
import xyz.noisif.buildconfig.alias.getLibrary
import xyz.noisif.buildconfig.libs
import java.io.File

class NsSpaTeaVmFrontendPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(target, PluginAlias.JAVA)
    target.pluginManager.apply(target, PluginAlias.TEAVM)

    target.tasks.withType<JavaCompile>().configureEach(
      TaskConfigureAction { task ->
        (task as JavaCompile).options.compilerArgs.add("-proc:none")
      },
    )
    val spaFrontendConfig = target.configurations.create("spaFrontend").apply {
      isCanBeResolved = true
      isCanBeConsumed = false
    }
    target.configurations.named("compileOnly").configure(
      ConfigurationConfigureAction { it.extendsFrom(spaFrontendConfig) },
    )
    target.configurations.matching { it.name == "teavm" }.configureEach(
      ConfigurationConfigureAction { it.extendsFrom(spaFrontendConfig) },
    )
    val extension = target.extensions.create<NsSpaTeaVmExtension>("nsFrontend").apply {
      title.convention("NOISIF SPA")
      generateHash.convention(true)
      minify.convention(true)
      devServer.host.convention("0.0.0.0")
      devServer.port.convention(3000)
      devServer.proxies.convention(emptyMap())
    }
    target.pluginManager.apply(target, PluginAlias.TEAVM)
    val teaVmTempDir = target.layout.buildDirectory.dir("teavm-temp")
    val teavmExt = target.extensions.getByType<TeaVMExtension>()
    val mainClazzProvider = extension.packageSuffix.zip(extension.mainClass) { suffix, clazz ->
      "${target.group}.nss.$suffix.$clazz"
    }
    teavmExt.all.apply {
      mainClass.set(mainClazzProvider)
    }
    teavmExt.js.apply {
      addedToWebApp.set(false)
      relativePathInOutputDir.set("")
      targetFileName.set("$TARGET_FILE_NAME.js")
      outputDir.set(teaVmTempDir)
    }
    val dependencyResources = target.configurations.named("spaFrontend").map { config ->
      config.incoming.artifactView(
        ArtifactViewConfigureAction { viewConfig ->
          viewConfig.attributes(
            AttributeContainerConfigureAction { attrs ->
              attrs.attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                target.objects.named<LibraryElements>(LibraryElements.RESOURCES),
              )
            },
          )
          viewConfig.componentFilter(ComponentFilterSpec { id -> id is ProjectComponentIdentifier })
        },
      ).files
    }
    val processFrontendTask = target.tasks.register<ProcessFrontendTask>("processFrontend") {
      dependsOn("generateJavaScript")

      inputJs.set(teaVmTempDir.map { it.file("$TARGET_FILE_NAME.js") })
      outputDir.set(project.layout.buildDirectory.dir("frontend-processed"))

      pageTitle.set(extension.title)
      minify.set(extension.minify)
      generateHash.set(extension.generateHash)

      spaResourceDirs.from(dependencyResources)

      val sourceSets = project.extensions.getByType<SourceSetContainer>()
      val mainResourcesDir = sourceSets["main"].resources.srcDirs.firstOrNull()
      if (mainResourcesDir != null) {
        val spaDir = File(mainResourcesDir, SPA_DIR)
        if (spaDir.exists()) {
          spaResourceDirs.from(spaDir)
        }
        val template = File(spaDir, INDEX_HTML)
        if (template.exists()) {
          htmlTemplate.set(template)
        }
      }
    }
    val buildFrontendTask = target.tasks.register<Sync>("buildFrontend") {
      dependsOn(processFrontendTask)

      val processedDir = project.layout.buildDirectory.dir("frontend-processed")
      val distDir = project.layout.buildDirectory.dir("dist")

      from(processedDir)

      val excludes = listOf(INDEX_HTML, "**/*.css")
      val sourceSets = project.extensions.getByType<SourceSetContainer>()
      from(
        sourceSets["main"].resources.srcDirs.map { File(it, SPA_DIR) },
        CopySpecConfigureAction { spec ->
          spec.exclude(excludes)
        },
      )
      from(
        dependencyResources.map { fileCollection ->
          fileCollection.files.map { dir ->
            if (dir.name == SPA_DIR) dir else File(dir, SPA_DIR)
          }.filter { it.exists() && it.isDirectory }
        },
        CopySpecConfigureAction { spec ->
          spec.exclude(excludes)
        },
      )
      into(distDir)
    }
    target.tasks.register<Exec>("watch") {
      val gradleRunScript = GradleRunScript.current()
      executable = target.rootProject.file(gradleRunScript.runScript).absolutePath
      args("buildFrontend", "-t")
      isIgnoreExitValue = true
    }
    val devServerConfig = target.configurations.create("devServerClasspath").apply {
      isCanBeResolved = true
      isCanBeConsumed = false
    }
    target.dependencies.add(devServerConfig.name, target.project(":nsl-serve"))
    target.dependencies.add(
      devServerConfig.name,
      target.libs.getLibrary(LibraryAlias.LOGBACK_CLASSIC),
    )
    target.tasks.register<DevServerTask>("devServer") {
      dependsOn(buildFrontendTask)
      classpath = devServerConfig

      host.set(extension.devServer.host)
      port.set(extension.devServer.port)
      proxies.set(extension.devServer.proxies)
      serveDir.set(project.layout.buildDirectory.dir("dist"))
    }
  }
}
