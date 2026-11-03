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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.dokka.gradle.DokkaExtension
import xyz.noisif.buildconfig.alias.PluginAlias
import xyz.noisif.buildconfig.alias.apply
import xyz.noisif.buildconfig.dokka.DokkaConfigureAction
import kotlin.jvm.java

class NsDokkaModulePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(target, PluginAlias.DOKKA)
    val extension = target.extensions.create("nsDokkaModule", NsDokkaModuleExtension::class.java)
    target.rootProject.dependencies.add("dokka", target)
    target.extensions.configure(
      DokkaExtension::class.java,
      DokkaConfigureAction { dokka ->
        dokka.moduleName.set(extension.moduleName.orElse(target.name))
        dokka.dokkaSourceSets.configureEach(DokkaSourceSetsConfigureAction(target, extension))
      },
    )
    target.afterEvaluate(DokkaModuleProjectAfterEvaluateAction())
  }
}
