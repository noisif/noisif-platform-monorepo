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

import java.io.File

interface EnvVar {
  val valueName: String
}

enum class ProjectEnvVar : EnvVar {
  VERSION,
  OSS_INDEX_USER,
  OSS_INDEX_TOKEN,
  ;

  override val valueName: String
    get() = name
}

object Env {
  @PublishedApi
  internal const val PREFIX = "NS"

  @PublishedApi
  internal val dotEnvMap: Map<String, String> by lazy {
    val envFile = File(findProjectRoot(), ".env")
    if (!envFile.exists()) {
      emptyMap()
    } else {
      envFile.readLines()
        .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
        .associate { line ->
          val parts = line.split("=", limit = 2)
          val key = parts[0].trim()
          val value = if (parts.size == 2) {
            parts[1].trim().removeSurrounding("\"")
              .removeSurrounding("'")
          } else {
            ""
          }
          key to value
        }
    }
  }

  inline fun <reified T> get(name: EnvVar, defValue: T): T {
    val key = "${PREFIX}_${name.valueName}"
    val rawValue = System.getenv(key) ?: dotEnvMap[key] ?: return defValue
    return convertValue<T>(rawValue, key)
  }

  inline fun <reified T> require(name: EnvVar): T {
    val key = "${PREFIX}_${name.valueName}"
    val rawValue = System.getenv(key)
      ?: dotEnvMap[key]
      ?: error(
        "build failed: environment variable $key is missing," +
          " please set it in .env file or environment variables",
      )
    return convertValue<T>(rawValue, key)
  }

  @PublishedApi
  internal inline fun <reified T> convertValue(value: String, key: String): T = try {
    when (T::class) {
      String::class -> value as T
      Int::class -> value.toInt() as T
      Boolean::class -> value.toBooleanStrict() as T
      Long::class -> value.toLong() as T
      Double::class -> value.toDouble() as T
      else -> error("unsupported type ${T::class.simpleName} for environment variable $key")
    }
  } catch (_: Exception) {
    error(
      "build failed: environment variable $key has invalid format for " +
        "type ${T::class.simpleName}, value: '$value'",
    )
  }

  private fun findProjectRoot(): File {
    var current: File? = File(System.getProperty("user.dir")).absoluteFile
    while (current != null) {
      if (File(current, "settings.gradle.kts").exists() || File(
          current,
          "settings.gradle",
        ).exists()
      ) {
        return current
      }
      current = current.parentFile
    }
    return File(System.getProperty("user.dir"))
  }
}
