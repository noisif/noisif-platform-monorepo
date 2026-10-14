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
package xyz.noisif.buildconfig.spa.teavm

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.JavaExec
import java.io.File
import java.util.Properties

internal abstract class DevServerTask : JavaExec() {
  @get:Input
  abstract val host: Property<String>

  @get:Input
  abstract val port: Property<Int>

  @get:Input
  abstract val proxies: MapProperty<String, NsProxyConfig>

  @get:InputDirectory
  abstract val serveDir: DirectoryProperty

  init {
    mainClass.set("xyz.noisif.nsl.serve.DevServerMain")
  }

  override fun exec() {
    val configFile = File(temporaryDir, "dev-server.properties")
    val props = Properties()

    props.setProperty("server.host", host.get())
    props.setProperty("server.port", port.get().toString())
    props.setProperty("server.dir", serveDir.get().asFile.absolutePath)

    var idx = 0
    proxies.get().forEach { (prefix, proxyConf) ->
      props.setProperty("proxy.$idx.prefix", prefix)
      props.setProperty("proxy.$idx.targetUrl", proxyConf.url.get())
      props.setProperty("proxy.$idx.stripPrefix", proxyConf.stripPrefix.get().toString())
      idx++
    }
    configFile.outputStream().use { props.store(it, "NSL DevServer Config") }
    args = listOf("--config", configFile.absolutePath)
    super.exec()
  }
}
