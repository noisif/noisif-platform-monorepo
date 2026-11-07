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

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import xyz.noisif.buildconfig.binary.NsBinaryResource
import xyz.noisif.buildconfig.service.docker.DockerArch
import javax.inject.Inject

abstract class DockerBinaryResource(name: String) : NsBinaryResource<DockerArch>(name)

abstract class NsDockerExtension @Inject constructor(objects: ObjectFactory, projectName: String) {
  abstract val baseImage: Property<String>
  abstract val imageName: Property<String>
  abstract val additionalTags: SetProperty<String>
  abstract val targetArchitecture: Property<DockerArch>

  val extraBinaries: NamedDomainObjectContainer<DockerBinaryResource> =
    objects.domainObjectContainer(DockerBinaryResource::class.java)

  init {
    baseImage.convention("gcr.io/distroless/java21:nonroot")
    imageName.convention(projectName)
    additionalTags.convention(emptySet())
    targetArchitecture.convention(DockerArch.current())
  }
}

fun Project.nsDockerImage(action: NsDockerExtension.() -> Unit) {
  extensions.configure(NsDockerExtension::class.java, action)
}
