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
package xyz.noisif.buildconfig.ffmpeg.libav

import xyz.noisif.buildconfig.HostPlatform
import xyz.noisif.buildconfig.PlatformSupported
import xyz.noisif.buildconfig.findCurrentPlatform

internal enum class LibAvBinariesPlatform(
  val platformKey: String,
  override vararg val supportedHosts: HostPlatform,
) : PlatformSupported {
  WINDOWS_64("windows-64", HostPlatform.WINDOWS_X86_64),
  OSX_64("osx-64", HostPlatform.MACOS_ARM64, HostPlatform.MACOS_X86_64),
  LINUX_64("linux-64", HostPlatform.LINUX_X86_64),
  ;

  companion object {
    fun current() = findCurrentPlatform<LibAvBinariesPlatform>()
  }
}
