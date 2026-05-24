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
package xyz.jwizard.jwl.kv.pubsub;

public enum TestKvChannel implements KvChannel {
    TEST_EVENTS("test:channel:events"),
    USER_NOTIFICATIONS("user:%s:notifications"),
    USER_EVENTS_WILDCARD("integration:users:*:events"),
    ;

    private final String pattern;

    TestKvChannel(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String buildChannel(Object... params) {
        return String.format(pattern, params);
    }
}
