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
import xyz.noisif.buildconfig.HostPlatform
import xyz.noisif.buildconfig.apply
import xyz.noisif.buildconfig.binary.NsBinariesPlugin
import xyz.noisif.buildconfig.binary.nsBinaries
import xyz.noisif.buildconfig.docker.NsDockerImagePlugin
import xyz.noisif.buildconfig.docker.nsDockerImage
import xyz.noisif.buildconfig.dokka.module.NsDokkaModulePlugin
import xyz.noisif.buildconfig.dokka.module.nsDokkaModule
import xyz.noisif.buildconfig.jvm.NsGC
import xyz.noisif.buildconfig.jvm.nsJvm
import xyz.noisif.buildconfig.service.NsServicePlugin
import xyz.noisif.buildconfig.service.docker.DockerArch
import xyz.noisif.buildconfig.service.nsService

pluginManager.apply<NsBinariesPlugin>()
pluginManager.apply<NsServicePlugin>()
pluginManager.apply<NsDockerImagePlugin>()
pluginManager.apply<NsDokkaModulePlugin>()

val ffmpegBuildsBase = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest"
val ffmpegMacOS = "https://evermeet.cx/ffmpeg/ffmpeg-8.1.zip"

nsBinaries {
  binaries {
    create("ffmpeg-tools") {
      urls.put(
        HostPlatform.WINDOWS_X86_64,
        "$ffmpegBuildsBase/ffmpeg-n8.1-latest-win64-gpl-8.1.zip",
      )
      urls.put(
        HostPlatform.WINDOWS_ARM64,
        "$ffmpegBuildsBase/ffmpeg-n8.1-latest-winarm64-gpl-8.1.zip",
      )
      urls.put(
        HostPlatform.LINUX_X86_64,
        "$ffmpegBuildsBase/ffmpeg-n8.1-latest-linux64-gpl-8.1.tar.xz",
      )
      urls.put(
        HostPlatform.LINUX_ARM64,
        "$ffmpegBuildsBase/ffmpeg-n8.1-latest-linuxarm64-gpl-8.1.tar.xz",
      )
      urls.put(HostPlatform.MACOS_X86_64, ffmpegMacOS)
      urls.put(HostPlatform.MACOS_ARM64, ffmpegMacOS)
      includes.addAll("ffmpeg", "ffmpeg.exe", "ffprobe", "ffprobe.exe")
    }
  }
}

nsService {
  jar {
    packageSuffix.set("transcoder")
    mainClass.set("NssTranscoderMain")
  }
  jvm {
    gc.set(NsGC.PARALLEL)
    extraArgs.add("-XX:ParallelGCThreads=4")
  }
}

nsDockerImage {
  extraBinaries {
    create("ffmpeg-static") {
      urls.put(DockerArch.AMD64, "$ffmpegBuildsBase/ffmpeg-n8.1-latest-linux64-gpl-8.1.tar.xz")
      urls.put(
        DockerArch.ARM64,
        "$ffmpegBuildsBase/ffmpeg-n8.1-latest-linuxarm64-gpl-8.1.tar.xz",
      )
      unpack.set(true)
      includes.addAll("ffmpeg", "ffprobe")
    }
  }
}

nsJvm {
  gc.set(NsGC.PARALLEL)
  // limit gc threads for preventing os threads starvation (ffmpeg processes)
  extraJvmArgs.add("-XX:ParallelGCThreads=4")
}

nsDokkaModule {
  moduleName.set("NSS Transcoder")
}

dependencies {
  implementation(libs.ffmpeg.wrapper)
  implementation(libs.protobuf.java)

  implementation(project(":nsl-codec"))
  implementation(project(":nsl-common"))
  implementation(project(":nsl-contracts"))
  implementation(project(":nsl-queue"))

  testImplementation(testFixtures(project(":nsl-common")))
}
