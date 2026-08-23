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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.java.archives.Manifest
import org.gradle.api.provider.Provider

internal class NsShadowJarConfigAction(
  private val project: Project,
  private val mainClazzProvider: Provider<String>,
) : Action<ShadowJar> {
  override fun execute(shadowTask: ShadowJar) {
    shadowTask.archiveFileName.set("${project.name}.jar")
    shadowTask.destinationDirectory.set(project.layout.projectDirectory.dir(".bin"))
    shadowTask.manifest(ManifestConfigAction(mainClazzProvider))
  }

  private class ManifestConfigAction(private val mainClazzProvider: Provider<String>) :
    Action<Manifest> {
    override fun execute(manifest: Manifest) {
      manifest.attributes(mapOf("Main-Class" to mainClazzProvider.get()))
    }
  }
}
