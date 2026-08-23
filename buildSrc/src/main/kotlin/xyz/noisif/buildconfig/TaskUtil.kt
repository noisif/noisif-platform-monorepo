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
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.SourceSet
import java.io.File

class TaskConfigureAction(private val action: (Task) -> Unit) : Action<Task> {
  override fun execute(task: Task) {
    action(task)
  }
}

class WithPluginAction(private val onPluginApplied: () -> Unit) : Action<AppliedPlugin> {
  override fun execute(appliedPlugin: AppliedPlugin) {
    onPluginApplied()
  }
}

class TaskNameStartsWithSpec(private val prefix: String) : Spec<Task> {
  override fun isSatisfiedBy(task: Task): Boolean = task.name.startsWith(prefix)
}

class SourceSetConfigureAction(private val generatedBaseDir: File) : Action<SourceSet> {
  override fun execute(sourceSet: SourceSet) {
    sourceSet.resources.srcDir(generatedBaseDir)
  }
}
