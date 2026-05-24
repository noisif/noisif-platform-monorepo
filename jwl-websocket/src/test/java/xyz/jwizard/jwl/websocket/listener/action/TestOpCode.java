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
package xyz.jwizard.jwl.websocket.listener.action;

import xyz.jwizard.jwl.codec.envelope.OpCode;

public enum TestOpCode implements OpCode {
    SUBSCRIBE(0x09, 0x01),
    SUBSCRIBE_ACK(0x09, 0x02),
    BROADCAST_MSG(0x09, 0x03);

    private final int code;

    TestOpCode(int category, int action) {
        this.code = OpCode.combine(category, action);
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name();
    }
}
