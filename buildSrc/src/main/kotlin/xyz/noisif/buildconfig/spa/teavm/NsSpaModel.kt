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

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.configure
import javax.inject.Inject

internal const val TARGET_FILE_NAME = "nss-app"
internal const val SPA_DIR = "spa"
internal const val INDEX_HTML = "index.html"

interface NsProxyConfig {
  @get:Input
  val url: Property<String>

  @get:Input
  val stripPrefix: Property<Boolean>
}

abstract class NsDevServerConfig @Inject constructor(private val objects: ObjectFactory) {
  abstract val host: Property<String>
  abstract val port: Property<Int>
  abstract val proxies: MapProperty<String, NsProxyConfig>

  fun proxy(prefix: String, action: NsProxyConfig.() -> Unit) {
    val proxyConf = objects.newInstance(NsProxyConfig::class.java)
    proxyConf.stripPrefix.convention(false)
    proxyConf.action()
    proxies.put(prefix, proxyConf)
  }
}

abstract class NsSpaTeaVmExtension @Inject constructor(objects: ObjectFactory) {
  abstract val title: Property<String>
  abstract val packageSuffix: Property<String>
  abstract val mainClass: Property<String>
  abstract val generateHash: Property<Boolean>
  abstract val minify: Property<Boolean>

  val devServer = objects.newInstance(NsDevServerConfig::class.java)

  fun devServer(action: NsDevServerConfig.() -> Unit) {
    devServer.action()
  }
}

fun Project.nsSpaTeaVm(action: NsSpaTeaVmExtension.() -> Unit) {
  configure<NsSpaTeaVmExtension>(action)
}

fun DependencyHandler.spaFrontend(dependencyNotation: Any): Dependency? =
  add("spaFrontend", dependencyNotation)
