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
package xyz.jwizard.jwl.codec.envelope;

public interface OpCode {
    static int combine(int category, int action) {
        return (category << 16) | (action & 0xFF);
    }

    int getCode();

    String getName();

    default String asString() {
        final int currentCode = getCode();
        final int category = (currentCode >> 16) & 0xFF;
        final int action = currentCode & 0xFF;
        return String.format("%s (0x%02X:%02X) [%d]", getName(), category, action, currentCode);
    }
}
