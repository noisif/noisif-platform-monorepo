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

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.configure

abstract class JwPolyglotJsExtension {
  abstract val nodeVersion: Property<String>
  abstract val entryPoints: MapProperty<String, String> // output name <-> entrypoint location
  abstract val npmDependencies: ListProperty<String> // additional node packages to install

  init {
    nodeVersion.convention("24.14.1")
    npmDependencies.convention(listOf("esbuild"))
  }
}

fun Project.jwPolyglotJs(action: JwPolyglotJsExtension.() -> Unit) {
  configure<JwPolyglotJsExtension>(action)
}
