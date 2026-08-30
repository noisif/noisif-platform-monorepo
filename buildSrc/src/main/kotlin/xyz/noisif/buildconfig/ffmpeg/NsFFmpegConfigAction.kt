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
package xyz.noisif.buildconfig.ffmpeg

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.plugins.ide.idea.model.IdeaModel
import xyz.noisif.buildconfig.TaskConfigureAction
import xyz.noisif.buildconfig.ffmpeg.libav.FFmpegDownloader
import xyz.noisif.buildconfig.ffmpeg.libav.FFprobeDownloader

internal class NsFFmpegConfigAction(private val extension: NsFFmpegExtension) : Action<Project> {
  override fun execute(project: Project) {
    registerTasks(project, extension)
    configureIdea(project, extension)
  }

  private fun registerTasks(project: Project, extension: NsFFmpegExtension) {
    val version = extension.version.get()
    val dirName = extension.directoryName.get()
    val targetDir = project.file("$dirName/$version")

    val downloadTask = project.tasks.register<DefaultTask>("downloadFFmpeg") {
      outputs.dir(targetDir).withPropertyName("outputDir")

      inputs.property("ffmpegVersion", version)
      inputs.property("needFfprobe", extension.downloadFfprobe.get())

      doLast {
        listOf(
          FFmpegDownloader(project, logger, version, targetDir),
          FFprobeDownloader(project, logger, version, targetDir) {
            extension.downloadFfprobe.get()
          },
        ).filter { it.isEnabled }.forEach { it.download() }
      }
    }
    project.tasks.named(
      "classes",
      TaskConfigureAction { task ->
        task.dependsOn(downloadTask)
      },
    )
  }

  private fun configureIdea(project: Project, extension: NsFFmpegExtension) {
    val dirName = extension.directoryName.get()
    project.plugins.withId(
      "idea",
      Action {
        val idea = project.extensions.getByType<IdeaModel>()
        idea.module.excludeDirs.remove(project.file(dirName))
      },
    )
  }
}
