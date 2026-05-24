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
package xyz.jwizard.jws.ingestor.scripting;

public enum TestScript implements ScriptFile {
    CLEANUP("scripting/test-check-cleanup.js"),
    CHECK_SIDE_EFFECT("scripting/test-check-side-effect.js"),
    EXECUTE("scripting/test-execute.js"),
    PRELOAD("scripting/test-preload.js"),
    SIDE_EFFECT("scripting/test-side-effect.js"),
    VARS("scripting/test-vars.js"),
    ;

    private final String code;

    TestScript(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
