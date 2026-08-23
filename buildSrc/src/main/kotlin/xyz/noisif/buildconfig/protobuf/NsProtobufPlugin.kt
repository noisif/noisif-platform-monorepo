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
package xyz.noisif.buildconfig.protobuf

import com.google.protobuf.gradle.ProtobufExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import xyz.noisif.buildconfig.alias.LibraryAlias
import xyz.noisif.buildconfig.alias.PluginAlias
import xyz.noisif.buildconfig.alias.apply
import xyz.noisif.buildconfig.alias.getLibrary
import xyz.noisif.buildconfig.libs

class NsProtobufPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(target, PluginAlias.PROTOBUF)
    target.pluginManager.apply(target, PluginAlias.IDEA)

    val protocLib = target.libs.getLibrary(LibraryAlias.PROTOBUF_COMPILER).get()
    val protocGroup = protocLib.module.group
    val protocName = protocLib.module.name
    val protocVersion = protocLib.versionConstraint.requiredVersion

    val protobufExt = target.extensions.getByType(ProtobufExtension::class.java)
    protobufExt.protoc(ProtocConfigAction(protocGroup, protocName, protocVersion))
    configureSourceSets(target)
  }

  private fun configureSourceSets(project: Project) {
    val sourceSets = project.extensions.getByType<SourceSetContainer>()
    for (sourceSet in sourceSets) {
      val generatedDir = project.file("build/generated/source/proto/${sourceSet.name}/java")
      sourceSet.java.srcDir(generatedDir)
    }
    project.plugins.withId("idea") {
      val idea = project.extensions.getByType<IdeaModel>()
      with(idea.module) {
        val mainProto = project.file("src/main/proto")
        if (mainProto.exists()) {
          sourceDirs.add(mainProto)
        }
        val testProto = project.file("src/test/proto")
        if (testProto.exists()) {
          testSources.from(testProto)
        }
        val fixturesProto = project.file("src/testFixtures/proto")
        if (fixturesProto.exists()) {
          testSources.from(fixturesProto)
        }
      }
    }
  }
}
