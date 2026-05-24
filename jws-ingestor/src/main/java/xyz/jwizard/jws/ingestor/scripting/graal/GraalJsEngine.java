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
package xyz.jwizard.jws.ingestor.scripting.graal;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import xyz.jwizard.jwl.common.bootstrap.lifecycle.IdempotentService;
import xyz.jwizard.jwl.common.util.io.IoUtil;
import xyz.jwizard.jws.ingestor.scripting.JsEngine;
import xyz.jwizard.jws.ingestor.scripting.ScriptFile;

public class GraalJsEngine extends IdempotentService implements JsEngine {
    private final List<ScriptFile> librariesToPreload;

    private Context context;

    private GraalJsEngine(Builder builder) {
        librariesToPreload = builder.libraries;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void onStart() throws Exception {
        log.info("Initializing GraalVM JS context, libraries to preload: {}",
            librariesToPreload.size());
        context = Context.newBuilder("js")
            .allowAllAccess(false)
            .build();
        for (final ScriptFile scriptFile : librariesToPreload) {
            log.debug("Preloading library: {}", scriptFile.getCode());
            final long start = System.currentTimeMillis();
            context.eval(buildSourceFromResource(scriptFile));
            log.trace("Library '{}' loaded in {} ms", scriptFile.getCode(),
                (System.currentTimeMillis() - start));
        }
        log.info("GraalVM JS context initialized successfully");
    }

    @Override
    protected void onStop() {
        IoUtil.closeQuietly(context, Context::close);
    }

    @Override
    public <T> T executeScript(ScriptFile scriptFile, Map<String, Object> variables,
                               Class<T> returnType) throws IOException {
        ensureRunning();
        log.debug("Executing script: '{}' with {} injected variables, expected return type: {}",
            scriptFile.getCode(), variables.size(), returnType.getSimpleName());
        if (log.isTraceEnabled()) {
            log.trace("Injected variable keys: {}", variables.keySet());
        }
        final Value bindings = context.getBindings("js");
        for (final Map.Entry<String, Object> entry : variables.entrySet()) {
            bindings.putMember(entry.getKey(), entry.getValue());
        }
        try {
            return doExecuteScript(scriptFile).as(returnType);
        } finally {
            for (final String key : variables.keySet()) {
                bindings.removeMember(key);
            }
            log.trace("Cleaned up injected variables for script: '{}'", scriptFile.getCode());
        }
    }

    @Override
    public <T> T executeScript(ScriptFile scriptFile, Class<T> returnType) throws IOException {
        log.debug("Executing script: '{}'. Expected return type: {}", scriptFile.getCode(),
            returnType.getSimpleName());
        return doExecuteScript(scriptFile).as(returnType);
    }

    @Override
    public void executeScript(ScriptFile scriptFile) throws IOException {
        log.debug("Executing script (fire-and-forget): '{}'", scriptFile.getCode());
        doExecuteScript(scriptFile);
    }

    @Override
    public <T> T callFunction(String functionName, Class<T> returnType, Object... args) {
        log.debug("Calling JS function: '{}' with {} arguments, expected return type: {}",
            functionName, args.length, returnType.getSimpleName());
        return doCallFunction(functionName, args).as(returnType);
    }

    @Override
    public void callFunction(String functionName, Object... args) {
        log.debug("Calling JS function (fire-and-forget): '{}' with {} arguments", functionName,
            args.length);
        doCallFunction(functionName, args);
    }

    private Value doExecuteScript(ScriptFile scriptFile) throws IOException {
        ensureRunning();
        log.trace("Evaluating source for script: '{}'", scriptFile.getCode());
        final long start = System.currentTimeMillis();
        final Value result = context.eval(buildSourceFromResource(scriptFile));
        log.trace("Script '{}' executed in {} ms", scriptFile.getCode(),
            (System.currentTimeMillis() - start));
        return result;
    }

    private Value doCallFunction(String functionName, Object... args) {
        ensureRunning();
        final Value function = context.getBindings("js").getMember(functionName);
        if (function == null || !function.canExecute()) {
            throw new IllegalArgumentException("Function '" + functionName + "' does not exist");
        }
        final long start = System.currentTimeMillis();
        final Value result = function.execute(args);
        log.trace("Function '{}' executed in {} ms", functionName,
            (System.currentTimeMillis() - start));
        return result;
    }

    private void ensureRunning() {
        Objects.requireNonNull(context, "JsEngine is not running");
    }

    private Source buildSourceFromResource(ScriptFile scriptFile) throws IOException {
        final URL resourceUrl = IoUtil.getRequiredResourceUrl(scriptFile.getCode());
        return Source.newBuilder("js", resourceUrl)
            .name(resourceUrl.getPath())
            .build();
    }

    public static class Builder {
        private final List<ScriptFile> libraries = new ArrayList<>();

        private Builder() {
        }

        public Builder withLibrary(ScriptFile scriptFile) {
            this.libraries.add(scriptFile);
            return this;
        }

        public GraalJsEngine build() {
            return new GraalJsEngine(this);
        }
    }
}
