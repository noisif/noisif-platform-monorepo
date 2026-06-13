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
package xyz.noisif.nsl.common.util.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.bootstrap.ForbiddenInstantiationException;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.function.Predicate;

public class IoUtil {
  private static final Logger LOG = LoggerFactory.getLogger(IoUtil.class);

  private IoUtil() {
    throw new ForbiddenInstantiationException(IoUtil.class);
  }

  public static void thrownQuietly(RunnableWithException runnableWithException) {
    if (runnableWithException == null) {
      return;
    }
    try {
      runnableWithException.run();
    } catch (Exception ex) {
      LOG.error("Cloud not perform action, cause: {}", ex.getMessage(), ex);
    }
  }

  public static <T> void closeQuietly(T resource, CloseAction<T> closeAction) {
    if (resource == null || closeAction == null) {
      return;
    }
    try {
      closeAction.perform(resource);
    } catch (Exception ex) {
      LOG.warn("Failed to close resource safely, cause: {}", ex.getMessage(), ex);
    }
  }

  public static void closeQuietly(Closeable closeable) {
    closeQuietly(closeable, AutoCloseable::close);
  }

  public static void closeQuietly(AutoCloseable closeable) {
    closeQuietly(closeable, AutoCloseable::close);
  }

  public static void closeQuietly(Runnable runnable) {
    closeQuietly(runnable, Runnable::run);
  }

  public static <T> void closeQuietly(
      T resource, Predicate<T> predicate, CloseAction<T> closeAction) {
    closeQuietly(
        resource,
        r -> {
          if (predicate.test(r)) {
            closeAction.perform(r);
          }
        });
  }

  public static String removeTrailingSlash(String path) {
    return path.startsWith("/") ? path.substring(1) : path;
  }

  // works safety between different modules without flat fat-jar structure
  public static URL getRequiredResourceUrl(String rawPath) throws IOException {
    final String path = removeTrailingSlash(rawPath);
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    final URL resourceUrl = classLoader.getResource(path);
    if (resourceUrl == null) {
      throw new IOException("Unable to find file on classpath: " + rawPath);
    }
    return resourceUrl;
  }
}
