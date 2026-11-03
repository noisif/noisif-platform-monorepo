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
package xyz.noisif.buildconfig.dokka.root

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Provider
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters
import org.jetbrains.dokka.gradle.formats.DokkaPublication
import xyz.noisif.buildconfig.dokka.DokkaConfigureAction
import xyz.noisif.buildconfig.dokka.DokkaHtmlParamsConfigureAction
import xyz.noisif.buildconfig.dokka.DokkaPublicationConfigureAction

internal class DokkaRootProjectAfterEvaluateAction : Action<Project> {
  override fun execute(project: Project) {
    val extension = project.extensions.findByType(NsDokkaRootExtension::class.java) ?: return
    project.extensions.configure(
      DokkaExtension::class.java,
      DokkaConfigureAction { dokka ->
        dokka.dokkaPublications.named(
          "html",
          DokkaPublication::class.java,
          DokkaPublicationConfigureAction { pub ->
            pub.outputDirectory.set(project.layout.buildDirectory.dir(extension.outputDir))
            val mdFile = project.file(extension.includeFileName.get())
            if (mdFile.exists()) {
              pub.includes.from(mdFile)
            }
          },
        )
        dokka.pluginsConfiguration.named(
          "html",
          DokkaHtmlPluginParameters::class.java,
          DokkaHtmlParamsConfigureAction { htmlParams ->
            addAssetIfExists(project, extension.iconPath, htmlParams.customAssets)
            addAssetIfExists(project, extension.bannerPath, htmlParams.customAssets)
            addAssetIfExists(project, extension.customStylePath, htmlParams.customStyleSheets)
            htmlParams.footerMessage.set(extension.copyrightMessage)
          },
        )
      },
    )
  }

  private fun addAssetIfExists(
    target: Project,
    assetPathProvider: Provider<String>,
    customAssets: ConfigurableFileCollection,
  ) {
    val assetFile = target.file(assetPathProvider.get())
    if (assetFile.exists()) {
      customAssets.from(assetFile)
    }
  }
}
