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

internal interface PlatformSupported {
  val supportedHosts: Array<out HostPlatform>
}

internal enum class HostPlatform(val isWindows: Boolean) {
  WINDOWS_X86_64(true),
  MACOS_ARM64(false),
  MACOS_X86_64(false),
  LINUX_X86_64(false),
  ;

  fun formatBinaryName(toolName: String): String = if (isWindows) "$toolName.exe" else toolName

  companion object {
    fun current(): HostPlatform {
      val osName = System.getProperty("os.name").lowercase()
      val arch = System.getProperty("os.arch").lowercase()
      return when {
        "win" in osName -> WINDOWS_X86_64
        "mac" in osName && ("aarch" in arch || "arm" in arch) -> MACOS_ARM64
        "mac" in osName -> MACOS_X86_64
        else -> LINUX_X86_64
      }
    }
  }
}

internal inline fun <reified T> findCurrentPlatform(): T where T : Enum<T>, T : PlatformSupported {
  val host = HostPlatform.current()
  return enumValues<T>().first { host in it.supportedHosts }
}
