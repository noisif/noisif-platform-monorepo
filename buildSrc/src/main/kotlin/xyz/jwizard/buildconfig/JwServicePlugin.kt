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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import kotlin.reflect.KProperty1

class JwServicePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val extension = target.extensions.create("jwService", JwServiceExtension::class.java)
    applyApplicationConventions(target, extension)
    target.afterEvaluate {
      extension.require(JwServiceExtension::packageSuffix, target.name)
      extension.require(JwServiceExtension::mainClass, target.name)
    }
  }

  private fun applyApplicationConventions(project: Project, jwExt: JwServiceExtension) {
    project.pluginManager.apply(
      project.libs
        .getPlugin("shadow")
        .get()
        .pluginId,
    )
    project.pluginManager.apply("application")
    project.dependencies {
      add("runtimeOnly", project.libs.getLibrary("logback.classic"))
    }
    val mainClazzProvider =
      jwExt.packageSuffix.zip(jwExt.mainClass) { suffix, clazz ->
        "${project.group}.jws.$suffix.$clazz"
      }
    project.configure<JavaApplication> {
      mainClass.set(mainClazzProvider)
    }
    project.tasks.withType<ShadowJar>().configureEach {
      archiveFileName.set("${project.name}.jar")
      destinationDirectory.set(project.layout.projectDirectory.dir(".bin"))
      manifest {
        attributes("Main-Class" to mainClazzProvider.get())
      }
    }
  }

  private fun JwServiceExtension.require(
    propertyRef: KProperty1<JwServiceExtension, Property<*>>,
    projectName: String,
  ) {
    if (!propertyRef.get(this).isPresent) {
      throw GradleException(
        "Error in '$projectName': Missing required value '${propertyRef.name}' in " +
          "jwizard { } block.",
      )
    }
  }
}
