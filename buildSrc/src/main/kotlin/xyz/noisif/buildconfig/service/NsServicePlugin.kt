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
package xyz.noisif.buildconfig.service

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.java.archives.Manifest
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import xyz.noisif.buildconfig.alias.LibraryAlias
import xyz.noisif.buildconfig.alias.PluginAlias
import xyz.noisif.buildconfig.alias.apply
import xyz.noisif.buildconfig.alias.getLibrary
import xyz.noisif.buildconfig.alias.getPluginId
import xyz.noisif.buildconfig.libs
import java.io.File
import kotlin.reflect.KProperty1

class NsServicePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val extension = target.extensions.create("nsService", NsServiceExtension::class.java)
    applyApplicationConventions(target, extension)
    target.afterEvaluate {
      extension.jar.require(NsJarExtension::packageSuffix, target.name)
      extension.jar.require(NsJarExtension::mainClass, target.name)
    }
  }

  private fun applyApplicationConventions(project: Project, nsExt: NsServiceExtension) {
    project.pluginManager.apply(project, PluginAlias.SHADOW)
    project.pluginManager.apply(project, PluginAlias.APPLICATION)
    project.pluginManager.apply(project, PluginAlias.IDEA)
    project.dependencies {
      add("runtimeOnly", project.libs.getLibrary(LibraryAlias.LOGBACK_CLASSIC))
    }
    val mainClazzProvider =
      nsExt.jar.packageSuffix.zip(nsExt.jar.mainClass) { suffix, clazz ->
        "${project.group}.nss.$suffix.$clazz"
      }
    project.configure<JavaApplication> {
      mainClass.set(mainClazzProvider)
    }
    project.tasks.withType(ShadowJar::class.java).configureEach(
      NsShadowJarConfigAction(project, mainClazzProvider, nsExt.jar.sourceDir),
    )
    project.tasks.withType(JavaExec::class.java).configureEach(
      NsJavaExecConfigAction(nsExt.jar.sourceDir),
    )
    project.plugins.withId(getPluginId(project, PluginAlias.IDEA)) {
      val idea = project.extensions.getByType<IdeaModel>()
      val ideaModule = idea.module
      ideaModule.excludeDirs.add(nsExt.jar.sourceDir.get().asFile)
    }
  }

  private class NsShadowJarConfigAction(
    private val project: Project,
    private val mainClazzProvider: Provider<String>,
    private val sourceDir: DirectoryProperty,
  ) : Action<ShadowJar> {
    override fun execute(shadowTask: ShadowJar) {
      shadowTask.archiveFileName.set("${project.name}.jar")
      shadowTask.destinationDirectory.set(sourceDir.asFile.get())
      shadowTask.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
      shadowTask.manifest(ManifestConfigAction(mainClazzProvider))
      shadowTask.mergeServiceFiles()
      shadowTask.exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    private class ManifestConfigAction(private val mainClazzProvider: Provider<String>) :
      Action<Manifest> {
      override fun execute(manifest: Manifest) {
        manifest.attributes(
          mapOf(
            "Main-Class" to mainClazzProvider.get(),
            "Multi-Release" to "true",
          ),
        )
      }
    }
  }

  private class NsJavaExecConfigAction(private val sourceDirProvider: Provider<Directory>) :
    Action<JavaExec> {
    override fun execute(task: JavaExec) {
      task.doFirst {
        val binDir = sourceDirProvider.get().asFile
        val currentPath = task.environment["PATH"] ?: System.getenv("PATH") ?: ""
        val pathSeparator = File.pathSeparator
        task.environment("PATH", "${binDir.absolutePath}$pathSeparator$currentPath")
        task.systemProperty("native.binary.dir", binDir.absolutePath)
      }
    }
  }

  private fun NsJarExtension.require(
    propertyRef: KProperty1<NsJarExtension, Property<*>>,
    projectName: String,
  ) {
    if (!propertyRef.get(this).isPresent) {
      throw GradleException(
        "Error in '$projectName': Missing required value '${propertyRef.name}' in " +
          "noisif { } block.",
      )
    }
  }
}
