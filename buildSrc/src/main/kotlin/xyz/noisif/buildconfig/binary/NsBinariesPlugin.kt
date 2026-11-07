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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.process.ExecOperations
import xyz.noisif.buildconfig.HostPlatform
import xyz.noisif.buildconfig.TaskConfigureAction
import xyz.noisif.buildconfig.alias.PluginAlias
import xyz.noisif.buildconfig.alias.apply
import xyz.noisif.buildconfig.alias.getPluginId
import java.io.File
import javax.inject.Inject

class NsBinariesPlugin @Inject constructor(
  private val execOps: ExecOperations,
  private val fsOps: FileSystemOperations,
  private val archOps: ArchiveOperations,
) : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(target, PluginAlias.IDEA)
    val extension = target.extensions.create("nsBinaries", NsBinariesExtension::class.java)
    val tmpDir = target.layout.buildDirectory.dir(".binaries-tmp").get().asFile
    val downloadTask = target.tasks.register(
      "prepareLocalBinaries",
      PrepareLocalBinariesConfigureAction(extension, tmpDir, execOps, fsOps, archOps),
    )
    target.tasks.named(
      "classes",
      TaskConfigureAction { it.dependsOn(downloadTask) },
    )
    target.plugins.withId(getPluginId(target, PluginAlias.IDEA)) {
      val idea = target.extensions.getByType<IdeaModel>()
      val ideaModule = idea.module
      ideaModule.excludeDirs.add(extension.targetDir.get().asFile)
    }
  }

  private class PrepareLocalBinariesConfigureAction(
    private val extension: NsBinariesExtension,
    private val tmpDir: File,
    private val execOps: ExecOperations,
    private val fsOps: FileSystemOperations,
    private val archOps: ArchiveOperations,
  ) : Action<Task> {
    override fun execute(task: Task) {
      task.group = "build setup"
      task.inputs.property("hostPlatform", HostPlatform.current().name)
      task.outputs.dir(extension.targetDir)
      task.doLast(PrepareLocalBinariesDoLastAction(extension, tmpDir, execOps, fsOps, archOps))
    }
  }

  private class PrepareLocalBinariesDoLastAction(
    private val extension: NsBinariesExtension,
    private val tmpDir: File,
    private val execOps: ExecOperations,
    private val fsOps: FileSystemOperations,
    private val archOps: ArchiveOperations,
  ) : Action<Task> {
    override fun execute(task: Task) {
      val logger = task.logger
      val outDir = extension.targetDir.get().asFile
      val processor = BinaryProcessor(execOps, fsOps, archOps, logger)
      val currentHost = HostPlatform.current()

      tmpDir.mkdirs()
      outDir.mkdirs()

      extension.binaries.forEach { binRes ->
        val url = binRes.urls.getting(currentHost).orNull
        if (url == null) {
          logger.info(
            "Skipping binary '${binRes.name}' - no URL configured for platform $currentHost",
          )
          return@forEach
        }
        val fileName = url.substringAfterLast("/")
        val shouldUnpack = binRes.unpack.orNull ?: processor.isArchive(fileName)
        processor.process(
          urlStr = url,
          resourceName = binRes.name,
          tmpDownloadDir = tmpDir,
          stagingDir = outDir,
          shouldUnpack = shouldUnpack,
          includesSet = binRes.includes.get(),
          excludesSet = binRes.excludes.get(),
          applyExecutablePermissions = !currentHost.isWindows,
        )
      }
    }
  }
}
