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

import java.io.IOException;
import java.util.Map;

public interface JsEngine {
    <T> T executeScript(ScriptFile scriptFile, Map<String, Object> variables,
                        Class<T> returnType) throws IOException;

    <T> T executeScript(ScriptFile scriptFile, Class<T> returnType) throws IOException;

    void executeScript(ScriptFile scriptFile) throws IOException;

    <T> T callFunction(String functionName, Class<T> returnType, Object... args);

    void callFunction(String functionName, Object... args);
}
