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
