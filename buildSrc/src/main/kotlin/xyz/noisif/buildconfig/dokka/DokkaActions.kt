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
package xyz.noisif.buildconfig.dokka

import org.gradle.api.Action
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters
import org.jetbrains.dokka.gradle.formats.DokkaPublication

internal class DokkaConfigureAction(private val action: (DokkaExtension) -> Unit) :
  Action<DokkaExtension> {
  override fun execute(dokka: DokkaExtension) {
    action(dokka)
  }
}

internal class DokkaPublicationConfigureAction(private val action: (DokkaPublication) -> Unit) :
  Action<DokkaPublication> {
  override fun execute(pub: DokkaPublication) {
    action(pub)
  }
}

internal class DokkaHtmlParamsConfigureAction(
  private val action: (DokkaHtmlPluginParameters) -> Unit,
) : Action<DokkaHtmlPluginParameters> {
  override fun execute(htmlParams: DokkaHtmlPluginParameters) {
    action(htmlParams)
  }
}
