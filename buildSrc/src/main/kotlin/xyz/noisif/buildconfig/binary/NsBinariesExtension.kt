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
package xyz.noisif.buildconfig.binary

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.configure
import xyz.noisif.buildconfig.HostPlatform
import javax.inject.Inject

abstract class LocalBinaryResource(name: String) : NsBinaryResource<HostPlatform>(name)

abstract class NsBinariesExtension @Inject constructor(
  objects: ObjectFactory,
  layout: ProjectLayout,
) {
  abstract val targetDir: DirectoryProperty

  val binaries = objects.domainObjectContainer(LocalBinaryResource::class.java)

  init {
    targetDir.convention(layout.projectDirectory.dir(".bin"))
  }
}

fun Project.nsBinaries(action: NsBinariesExtension.() -> Unit) {
  configure<NsBinariesExtension>(action)
}
