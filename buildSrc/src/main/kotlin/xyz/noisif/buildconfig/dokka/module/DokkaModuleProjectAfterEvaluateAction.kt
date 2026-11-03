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
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters
import xyz.noisif.buildconfig.dokka.DokkaConfigureAction
import xyz.noisif.buildconfig.dokka.DokkaHtmlParamsConfigureAction
import xyz.noisif.buildconfig.dokka.root.NsDokkaRootExtension

internal class DokkaModuleProjectAfterEvaluateAction : Action<Project> {
  override fun execute(project: Project) {
    val rootExtension = project.rootProject.extensions.findByType(NsDokkaRootExtension::class.java)
    project.extensions.configure(
      DokkaExtension::class.java,
      DokkaConfigureAction { dokka ->
        dokka.pluginsConfiguration.named(
          "html",
          DokkaHtmlPluginParameters::class.java,
          DokkaHtmlParamsConfigureAction { htmlParams ->
            if (rootExtension != null) {
              htmlParams.footerMessage.set(rootExtension.copyrightMessage)
            }
          },
        )
      },
    )
  }
}
