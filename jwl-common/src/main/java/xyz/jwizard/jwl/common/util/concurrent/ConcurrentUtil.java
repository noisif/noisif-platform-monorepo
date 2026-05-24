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
package xyz.jwizard.jwl.common.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import xyz.jwizard.jwl.common.bootstrap.ForbiddenInstantiationException;
import xyz.jwizard.jwl.common.util.thread.ThreadUtil;

public class ConcurrentUtil {
    private ConcurrentUtil() {
        throw new ForbiddenInstantiationException(ConcurrentUtil.class);
    }

    public static void await(Consumer<IoCallback> action) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        final IoCallback callback = new IoCallback() {
            @Override
            public void onSuccess() {
                future.complete(null);
            }

            @Override
            public void onFailure(Throwable cause) {
                future.completeExceptionally(cause);
            }
        };
        try {
            action.accept(callback);
            future.join();
        } catch (CompletionException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeEx) {
                throw runtimeEx;
            }
            throw new ConcurrentOperationException(cause);
        }
    }

    public static ScheduledExecutorService singleThread(String name) {
        return Executors.newSingleThreadScheduledExecutor(ThreadUtil.createThreadFactory(name));
    }
}
