/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
