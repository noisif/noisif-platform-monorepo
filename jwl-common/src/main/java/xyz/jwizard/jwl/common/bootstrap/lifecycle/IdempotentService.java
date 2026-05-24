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
package xyz.jwizard.jwl.common.bootstrap.lifecycle;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.bootstrap.CriticalBootstrapException;

public abstract class IdempotentService implements Closeable {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public final void start() {
        if (initialized.compareAndSet(false, true)) {
            running.set(true);
            try {
                onStart();
                log.info("Service '{}' started successfully", getClass().getSimpleName());
            } catch (Exception ex) {
                running.set(false);
                initialized.set(false);
                throw translateException(ex);
            }
        } else {
            log.warn("Service '{}' is already running or was already initialized",
                getClass().getSimpleName());
        }
    }

    @Override
    public final void close() {
        if (running.compareAndSet(true, false)) {
            onStop();
        }
    }

    protected abstract void onStart() throws Exception;

    protected abstract void onStop();

    // available to overwrite for non-critical errors
    protected RuntimeException translateException(Exception ex) {
        return new CriticalBootstrapException("Failed to start service: " +
            getClass().getSimpleName(), ex);
    }
}
