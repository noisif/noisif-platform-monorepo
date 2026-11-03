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
package xyz.noisif.buildconfig.dokka.module

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

class DokkaSourceSetsConfigureAction(
  private val project: Project,
  private val extension: NsDokkaModuleExtension,
) : Action<DokkaSourceSetSpec> {
  override fun execute(spec: DokkaSourceSetSpec) {
    val moduleDocFile = project.file(extension.includeFileName.get())
    if (moduleDocFile.exists()) {
      spec.includes.from(moduleDocFile)
    }
    spec.jdkVersion.set(resolveJavaVersion())
    spec.documentedVisibilities.set(
      setOf(
        VisibilityModifier.Public,
        VisibilityModifier.Protected,
        VisibilityModifier.Package,
        VisibilityModifier.Private,
      ),
    )
  }

  private fun resolveJavaVersion(): Int {
    val javaExt = project.extensions.findByType<JavaPluginExtension>()
    return javaExt?.toolchain?.languageVersion?.orNull?.asInt()
      ?: javaExt?.targetCompatibility?.majorVersion?.toIntOrNull()
      ?: error("Unable to get JVM version")
  }
}
