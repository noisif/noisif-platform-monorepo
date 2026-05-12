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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.bootstrap.ForbiddenInstantiationException;
import xyz.jwizard.jwl.common.util.io.RunnableWithException;

public class ThreadUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadUtil.class);

    private ThreadUtil() {
        throw new ForbiddenInstantiationException(ThreadUtil.class);
    }

    public static void runAsync(String name, RunnableWithException task) {
        Thread.ofVirtual()
            .name(name)
            .start(() -> {
                try {
                    task.run();
                } catch (Exception ex) {
                    LOG.error("Critical error in async task: {}", name, ex);
                }
            });
    }
}
