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
package xyz.noisif.buildconfig.service

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.configure
import xyz.noisif.buildconfig.jvm.NsGC
import xyz.noisif.buildconfig.overrideStringProperty
import javax.inject.Inject

abstract class NsJarExtension @Inject constructor(layout: ProjectLayout) {
  abstract val packageSuffix: Property<String>
  abstract val mainClass: Property<String>
  abstract val sourceDir: DirectoryProperty

  init {
    sourceDir.convention(layout.projectDirectory.dir(".jar"))
  }
}

abstract class NsJvmExtension {
  abstract val gc: Property<NsGC>
  abstract val xms: Property<String>
  abstract val xmx: Property<String>
  abstract val maxMetaspaceSize: Property<String>
  abstract val maxDirectMemorySize: Property<String>
  abstract val extraArgs: ListProperty<String>

  init {
    gc.convention(NsGC.ZGC)
    xms.convention("512m")
    xmx.convention("2g")
    maxMetaspaceSize.convention("512m")
    maxDirectMemorySize.convention("1g")
    extraArgs.convention(emptyList())
  }
}

abstract class NsServiceExtension @Inject constructor(
  objects: ObjectFactory,
  layout: ProjectLayout,
) {
  val jar: NsJarExtension = objects.newInstance(NsJarExtension::class.java, layout)
  val jvm: NsJvmExtension = objects.newInstance(NsJvmExtension::class.java)

  fun jar(action: Action<NsJarExtension>) {
    action.execute(jar)
  }

  fun jvm(action: Action<NsJvmExtension>) {
    action.execute(jvm)
  }
}

fun Project.nsService(action: NsServiceExtension.() -> Unit) {
  configure<NsServiceExtension> {
    overrideStringProperty("jvm.xms", jvm.xms)
    overrideStringProperty("jvm.xmx", jvm.xmx)
    overrideStringProperty("jvm.maxMetaspaceSize", jvm.maxMetaspaceSize)
    overrideStringProperty("jvm.maxDirectMemorySize", jvm.maxDirectMemorySize)
    action()
  }
}
