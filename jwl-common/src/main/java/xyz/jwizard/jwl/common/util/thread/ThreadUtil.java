/*
 * Copyright (c) 2022-2026 JWizard. All Rights Reserved.
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
package xyz.jwizard.jwl.common.util.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.bootstrap.ForbiddenInstantiationException;
import xyz.jwizard.jwl.common.util.io.RunnableWithException;

import java.util.concurrent.ThreadFactory;

public class ThreadUtil {
  private static final Logger LOG = LoggerFactory.getLogger(ThreadUtil.class);

  private ThreadUtil() {
    throw new ForbiddenInstantiationException(ThreadUtil.class);
  }

  public static void runAsync(String name, RunnableWithException task) {
    Thread.ofVirtual()
        .name(name)
        .start(
            () -> {
              try {
                task.run();
              } catch (Exception ex) {
                LOG.error("Critical error in async task: {}", name, ex);
              }
            });
  }

  public static ThreadFactory createThreadFactory(String name) {
    return new DefaultThreadFactory(name);
  }
}
