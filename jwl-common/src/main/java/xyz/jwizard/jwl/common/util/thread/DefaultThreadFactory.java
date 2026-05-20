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
package xyz.jwizard.jwl.common.util.thread;

import java.util.concurrent.ThreadFactory;

import org.jspecify.annotations.NonNull;

class DefaultThreadFactory implements ThreadFactory {
    private final String threadName;

    DefaultThreadFactory(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public Thread newThread(@NonNull Runnable runnable) {
        final Thread thread = new Thread(runnable, threadName);
        thread.setDaemon(true);
        return thread;
    }
}
