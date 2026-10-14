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
package xyz.noisif.buildconfig

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.util.PatternFilterable
import java.io.File

internal class TaskConfigureAction(private val action: (Task) -> Unit) : Action<Task> {
  override fun execute(task: Task) {
    action(task)
  }
}

internal class WithPluginAction(private val onPluginApplied: () -> Unit) : Action<AppliedPlugin> {
  override fun execute(appliedPlugin: AppliedPlugin) {
    onPluginApplied()
  }
}

internal class TaskNameStartsWithSpec(private val prefix: String) : Spec<Task> {
  override fun isSatisfiedBy(task: Task): Boolean = task.name.startsWith(prefix)
}

internal class SourceSetConfigureAction(private val generatedBaseDir: File) : Action<SourceSet> {
  override fun execute(sourceSet: SourceSet) {
    sourceSet.resources.srcDir(generatedBaseDir)
  }
}

internal class CopySpecConfigureAction(private val action: (CopySpec) -> Unit) : Action<CopySpec> {
  override fun execute(copySpec: CopySpec) {
    action(copySpec)
  }
}

internal class AddDependencyAction<T : Task>(
  private val taskProvider: TaskProvider<T>,
  private val excludePattern: String = "",
) : Action<Task> {
  override fun execute(task: Task) {
    task.dependsOn(taskProvider)
    if (excludePattern.isNotEmpty() && task is PatternFilterable) {
      task.exclude(excludePattern)
    }
  }
}

internal class ArtifactViewConfigureAction(
  private val action: (ArtifactView.ViewConfiguration) -> Unit,
) : Action<ArtifactView.ViewConfiguration> {
  override fun execute(viewConfig: ArtifactView.ViewConfiguration) {
    action(viewConfig)
  }
}

internal class AttributeContainerConfigureAction(private val action: (AttributeContainer) -> Unit) :
  Action<AttributeContainer> {
  override fun execute(attrs: AttributeContainer) {
    action(attrs)
  }
}

internal class ComponentFilterSpec(private val condition: (ComponentIdentifier) -> Boolean) :
  Spec<ComponentIdentifier> {
  override fun isSatisfiedBy(id: ComponentIdentifier): Boolean = condition(id)
}

internal class ConfigurationConfigureAction(private val action: (Configuration) -> Unit) :
  Action<Configuration> {
  override fun execute(config: Configuration) {
    action(config)
  }
}
