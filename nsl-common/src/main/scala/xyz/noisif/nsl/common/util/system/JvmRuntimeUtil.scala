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
package xyz.noisif.nsl.common.util.system

import org.slf4j.LoggerFactory

import xyz.noisif.nsl.common.util.parser.TypeEnsureParser

import java.lang.management.ManagementFactory
import java.util.function.{Function => JFunction}

object JvmRuntimeUtil {
  private val LOG = LoggerFactory.getLogger(getClass)

  def getJvmArg[T](arg: JvmArg, defaultValue: T): T = {
    val prefix = arg.getPrefix
    findRawArgument(prefix) match {
      case Some(rawValue) =>
        LOG.trace("found jvm argument match for {}: {}, transforming", prefix, rawValue)
        val transformer = arg.getTransformer.asInstanceOf[JFunction[String, Any]]
        TypeEnsureParser.parseAndCast(rawValue, transformer, arg.getType, defaultValue)
      case None =>
        LOG.trace("jvm argument {} not found in startup flags", prefix)
        defaultValue
    }
  }

  def getJvmArg[T](arg: DefaultJvmArg): T = getJvmArg(arg, null.asInstanceOf[T])

  private def findRawArgument(prefix: String): Option[String] = {
    val iterator = ManagementFactory.getRuntimeMXBean.getInputArguments.iterator()
    while (iterator.hasNext) {
      val jvmInputArg = iterator.next()
      if (jvmInputArg.startsWith(prefix)) {
        return Some(jvmInputArg.substring(prefix.length))
      }
    }
    None
  }
}
