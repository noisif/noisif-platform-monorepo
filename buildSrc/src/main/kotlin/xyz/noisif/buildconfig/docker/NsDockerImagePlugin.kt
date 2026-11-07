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
package xyz.noisif.buildconfig.docker

import com.google.cloud.tools.jib.gradle.JibExtension
import com.google.cloud.tools.jib.gradle.PlatformParameters
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import xyz.noisif.buildconfig.CopySpecConfigureAction
import xyz.noisif.buildconfig.alias.PluginAlias
import xyz.noisif.buildconfig.alias.apply
import xyz.noisif.buildconfig.binary.BinaryProcessor
import xyz.noisif.buildconfig.overrideEnumProperty
import xyz.noisif.buildconfig.service.NsServiceExtension
import java.io.File
import javax.inject.Inject
import kotlin.jvm.java

class NsDockerImagePlugin @Inject constructor(
  private val execOps: ExecOperations,
  private val fsOps: FileSystemOperations,
  private val archOps: ArchiveOperations,
) : Plugin<Project> {

  companion object {
    private const val TEMP_ROOT_DIR = ".jib"
    private const val TEMP_DOWNLOADS = "$TEMP_ROOT_DIR/tmp"
    private const val TEMP_EXTRA_BINARIES = "$TEMP_ROOT_DIR/binaries"
    private const val TEMP_DOCKER_ROOT = "$TEMP_ROOT_DIR/docker-root"
  }

  override fun apply(target: Project) {
    val dockerExt = target.extensions.create(
      "nsDocker",
      NsDockerExtension::class.java,
      target.objects,
      target.name,
    )
    target.overrideEnumProperty("arch", dockerExt.targetArchitecture)
    target.pluginManager.apply(target, PluginAlias.JIB)

    val tmpDownloadDir = target.layout.buildDirectory.dir(TEMP_DOWNLOADS).get().asFile
    val stagingDir = target.layout.buildDirectory.dir(TEMP_EXTRA_BINARIES).get().asFile
    val dockerRoot = target.layout.buildDirectory.dir(TEMP_DOCKER_ROOT).get().asFile

    val downloadTask = target.tasks.register(
      "prepareExtraBinaries",
      PrepareExtraBinariesConfigureAction(dockerExt, tmpDownloadDir, stagingDir),
    )
    val serviceExt = target.extensions.getByType(NsServiceExtension::class.java)
    val assembleJibTask = target.tasks.register(
      "assembleJibFilesystem",
      Sync::class.java,
      AssembleJibFilesAction(target, stagingDir, dockerRoot, downloadTask, serviceExt),
    )
    target.afterEvaluate(
      ConfigureJibAfterEvaluateAction(dockerExt, serviceExt, dockerRoot, assembleJibTask),
    )
  }

  private inner class PrepareExtraBinariesConfigureAction(
    private val extension: NsDockerExtension,
    private val tmpDownloadDir: File,
    private val stagingDir: File,
  ) : Action<Task> {
    override fun execute(task: Task) {
      task.inputs.property("targetArch", extension.targetArchitecture.map { it.name })
      task.outputs.dir(stagingDir)
      task.doLast(PrepareExtraBinariesDoLastAction(extension, tmpDownloadDir, stagingDir))
    }
  }

  private inner class PrepareExtraBinariesDoLastAction(
    private val extension: NsDockerExtension,
    private val tmpDownloadDir: File,
    private val stagingDir: File,
  ) : Action<Task> {
    override fun execute(task: Task) {
      val targetArch = extension.targetArchitecture.get()
      val processor = BinaryProcessor(execOps, fsOps, archOps, task.logger)

      tmpDownloadDir.mkdirs()
      stagingDir.mkdirs()

      extension.extraBinaries.forEach { binRes ->
        val urlStr = binRes.urls.getting(targetArch).orNull ?: throw GradleException(
          "No URL for architecture '${targetArch.name}' in docker resource '${binRes.name}'.",
        )
        val fileName = urlStr.substringAfterLast("/")
        val shouldUnpack = binRes.unpack.orNull ?: processor.isArchive(fileName)
        processor.process(
          urlStr = urlStr,
          resourceName = binRes.name,
          tmpDownloadDir = tmpDownloadDir,
          stagingDir = stagingDir,
          shouldUnpack = shouldUnpack,
          includesSet = binRes.includes.get(),
          excludesSet = binRes.excludes.get(),
          applyExecutablePermissions = true,
        )
      }
    }
  }

  private class AssembleJibFilesAction(
    private val target: Project,
    private val stagingDir: File,
    private val dockerRoot: File,
    private val downloadTask: TaskProvider<Task>,
    private val serviceExtension: NsServiceExtension,
  ) : Action<Sync> {
    override fun execute(sync: Sync) {
      sync.dependsOn(downloadTask)
      target.tasks.findByName("shadowJar")?.let { shadowTask ->
        sync.dependsOn(shadowTask)
      }
      sync.into(dockerRoot)
      val jarSource = serviceExtension.jar.sourceDir.get().asFile
      sync.from(
        jarSource,
        CopySpecConfigureAction {
          it.into("app")
        },
      )
      sync.from(
        stagingDir,
        CopySpecConfigureAction {
          it.into("extra-bin")
        },
      )
    }
  }

  private class ConfigureJibAfterEvaluateAction(
    private val dockerExtension: NsDockerExtension,
    private val serviceExtension: NsServiceExtension,
    private val dockerRoot: File,
    private val assembleJibTask: TaskProvider<out Task>,
  ) : Action<Project> {
    override fun execute(project: Project) {
      val jib = project.extensions.getByType(JibExtension::class.java)
      val archEnum = dockerExtension.targetArchitecture.get()

      jib.from.image = dockerExtension.baseImage.get()
      jib.from.platforms.add(
        PlatformParameters().apply {
          os = "linux"
          architecture = archEnum.jibArchitecture
        },
      )
      jib.to.image = dockerExtension.imageName.get()

      val extraTags = dockerExtension.additionalTags.get()
      if (extraTags.isNotEmpty()) {
        jib.to.tags.addAll(extraTags)
      }

      val jvmExt = serviceExtension.jvm
      val minMem = jvmExt.xms.get()
      val maxMem = jvmExt.xmx.get()
      val metaspace = jvmExt.maxMetaspaceSize.orNull
      val directMemory = jvmExt.maxDirectMemorySize.orNull
      val gc = jvmExt.gc.get()
      val extraArgs = jvmExt.extraArgs.get()

      jib.container.entrypoint = mutableListOf<String>().apply {
        add("java")
        add("-Xms$minMem")
        add("-Xmx$maxMem")
        if (!metaspace.isNullOrBlank()) {
          add("-XX:MaxMetaspaceSize=$metaspace")
        }
        if (!directMemory.isNullOrBlank()) {
          add("-XX:MaxDirectMemorySize=$directMemory")
        }
        addAll(gc.jvmFlags)
        addAll(extraArgs)
        add("-jar")
        add("/app/${project.name}.jar")
      }
      jib.container.environment =
        mapOf("PATH" to "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/extra-bin")

      jib.extraDirectories.setPaths(listOf(dockerRoot.absolutePath))
      jib.extraDirectories.permissions.set(mapOf("/extra-bin/*" to "755"))

      project.tasks.findByName("jib")?.dependsOn(assembleJibTask)
      project.tasks.findByName("jibDockerBuild")?.dependsOn(assembleJibTask)
      project.tasks.findByName("jibBuildTar")?.dependsOn(assembleJibTask)
    }
  }
}
