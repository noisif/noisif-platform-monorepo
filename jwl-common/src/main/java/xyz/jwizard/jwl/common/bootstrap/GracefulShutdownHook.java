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
package xyz.jwizard.jwl.common.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.jwizard.jwl.common.util.io.IoUtil;

public class GracefulShutdownHook extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(GracefulShutdownHook.class);

    private final List<? extends LifecycleHook> hooks;
    private final CountDownLatch shutdownLatch;
    private final boolean wait;

    public GracefulShutdownHook(List<? extends LifecycleHook> hooks, CountDownLatch shutdownLatch,
                                boolean wait) {
        super("shutdown-t");
        this.hooks = hooks;
        this.shutdownLatch = shutdownLatch;
        this.wait = wait;
    }

    @Override
    public void run() {
        LOG.info("Initiating graceful shutdown sequence");
        final List<LifecycleHook> stopOrder = new ArrayList<>(hooks);
        Collections.reverse(stopOrder);
        for (final LifecycleHook hook : stopOrder) {
            final String hookName = hook.getClass().getSimpleName();
            LOG.info("Stopping component: [{}]", hookName);
            IoUtil.closeQuietly(hook, LifecycleHook::onStop);
        }
        LOG.info("Graceful shutdown sequence completed");
        if (wait) {
            shutdownLatch.countDown();
        }
    }
}
