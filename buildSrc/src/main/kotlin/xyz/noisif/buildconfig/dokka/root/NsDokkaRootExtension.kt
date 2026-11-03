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

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.configure

abstract class NsDokkaRootExtension {
  abstract val projectName: Property<String>
  abstract val documentationVersion: Property<String>
  abstract val outputDir: Property<String>
  abstract val iconPath: Property<String>
  abstract val bannerPath: Property<String>
  abstract val customStylePath: Property<String>
  abstract val copyrightMessage: Property<String>
  abstract val includeFileName: Property<String>

  init {
    projectName.convention("DOCUMENTATION")
    documentationVersion.convention("1.0.0-SNAPSHOT")
    outputDir.convention("docs/dokka")
    iconPath.convention("dokka/logo-icon.svg")
    bannerPath.convention("dokka/banner.png")
    customStylePath.convention("dokka/styles.css")
    copyrightMessage.convention("")
    includeFileName.convention("DOKKA.md")
  }
}

fun Project.nsDokkaRoot(action: NsDokkaRootExtension.() -> Unit) {
  configure<NsDokkaRootExtension>(action)
}
