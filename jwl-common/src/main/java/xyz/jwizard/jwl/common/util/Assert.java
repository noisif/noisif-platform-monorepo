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
package xyz.jwizard.jwl.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import xyz.jwizard.jwl.common.bootstrap.CriticalBootstrapException;
import xyz.jwizard.jwl.common.bootstrap.ForbiddenInstantiationException;

public class Assert {
    private Assert() {
        throw new ForbiddenInstantiationException(Assert.class);
    }

    public static <T> void notNull(T object, String message) {
        state(object != null, message);
    }

    public static <T> T notNullAndGet(T object, String message) {
        state(object != null, message);
        return object;
    }

    public static void notEmpty(Collection<?> collection, String message) {
        state(collection != null && !collection.isEmpty(), message);
    }

    public static void notNullAll(Collection<?> collection, String message) {
        state(collection != null && collection.stream().allMatch(Objects::nonNull), message);
    }

    public static void notNullAll(Map<?, ?> collection, String message) {
        notNullAll(collection.values(), message);
    }

    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new CriticalBootstrapException(message);
        }
    }
}
