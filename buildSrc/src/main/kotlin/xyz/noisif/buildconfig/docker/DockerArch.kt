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
package xyz.noisif.buildconfig.service.docker

import xyz.noisif.buildconfig.HostPlatform
import xyz.noisif.buildconfig.PlatformSupported
import xyz.noisif.buildconfig.findCurrentPlatform

enum class DockerArch(
  val jibArchitecture: String,
  override val supportedHosts: Array<out HostPlatform>,
) : PlatformSupported {
  AMD64(
    "amd64",
    arrayOf(
      HostPlatform.WINDOWS_X86_64,
      HostPlatform.MACOS_X86_64,
      HostPlatform.LINUX_X86_64,
    ),
  ),
  ARM64(
    "arm64",
    arrayOf(
      HostPlatform.MACOS_ARM64,
    ),
  ),
  ;

  companion object {
    fun current() = findCurrentPlatform<DockerArch>()
  }
}
