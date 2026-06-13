/*
 * Copyright (c) 2022-2026 NOISIF. All Rights Reserved.
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
package xyz.noisif.nss.ingestor.scripting.graal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.noisif.nsl.common.util.CastUtil;
import xyz.noisif.nss.ingestor.scripting.ScriptFile;
import xyz.noisif.nss.ingestor.scripting.TestScript;

import java.util.Map;

class GraalJsEngineTest {
  private GraalJsEngine engine;

  @BeforeEach
  void setUp() {
    engine = GraalJsEngine.builder().withLibrary(TestScript.PRELOAD).build();
  }

  @AfterEach
  void tearDown() {
    engine.close();
  }

  @Test
  @DisplayName("should throw NullPointerException when using engine before calling start()")
  void shouldThrowWhenUsingEngineBeforeStart() {
    assertThatThrownBy(() -> engine.executeScript(TestScript.EXECUTE, String.class))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("JsEngine is not running");
  }

  @Test
  @DisplayName("should preload configured libraries into global scope on start")
  void shouldPreloadLibrariesOnStart() {
    // given
    engine.start();
    // when
    final Integer result = engine.callFunction("multiply", Integer.class, 3, 4);
    // then
    assertThat(result).isEqualTo(12);
  }

  @Test
  @DisplayName("should execute a simple script and return the expected result")
  void shouldExecuteSimpleScriptAndReturnResult() throws Exception {
    // given
    engine.start();
    // when
    final String result = engine.executeScript(TestScript.EXECUTE, String.class);
    // then
    assertThat(result).isEqualTo("Hello from script");
  }

  @Test
  @DisplayName("should execute script correctly using injected Java variables")
  void shouldExecuteScriptWithInjectedVariables() throws Exception {
    // given
    engine.start();
    final Map<String, Object> vars =
        Map.of(
            "injectedA", 15,
            "injectedB", 25);
    // when
    final Integer result = engine.executeScript(TestScript.VARS, vars, Integer.class);
    // then
    assertThat(result).isEqualTo(40);
  }

  @Test
  @DisplayName("should clean up injected variables from global scope after execution")
  void shouldCleanupInjectedVariablesAfterExecution() throws Exception {
    // given
    engine.start();
    final Map<String, Object> vars =
        Map.of(
            "injectedA", 10,
            "injectedB", 20);
    // when
    engine.executeScript(TestScript.VARS, vars, Integer.class);
    final String typeofInjectedA = engine.executeScript(TestScript.CLEANUP, String.class);
    // then
    assertThat(typeofInjectedA).isEqualTo("undefined");
  }

  @Test
  @DisplayName("should safely clean up variables even if the script throws an exception")
  void shouldCleanupVariablesEvenIfScriptThrowsException() throws Exception {
    // given
    engine.start();
    final Map<String, Object> vars = Map.of("injectedA", "Will Fail");
    // when & then
    assertThatThrownBy(() -> engine.executeScript(TestScript.VARS, vars, Integer.class))
        .isInstanceOf(Exception.class);
    final String typeofInjectedA = engine.executeScript(TestScript.CLEANUP, String.class);
    assertThat(typeofInjectedA).isEqualTo("undefined");
  }

  @Test
  @DisplayName("should correctly call preloaded JS function with provided arguments")
  void shouldCallPreloadedFunctionWithArguments() {
    // given
    engine.start();
    // when
    final Integer result = engine.callFunction("multiply", Integer.class, 5, 4);
    // then
    assertThat(result).isEqualTo(20);
  }

  @Test
  @DisplayName("should throw IllegalArgumentException when calling a non-existent JS function")
  void shouldThrowWhenCallingNonExistentFunction() {
    // given
    engine.start();
    // when & then
    assertThatThrownBy(() -> engine.callFunction("nonExistentFunc", String.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not exist");
  }

  @Test
  @DisplayName("should execute script without returning result")
  void shouldExecuteScriptWithoutReturn() throws Exception {
    // given
    engine.start();
    // when
    engine.executeScript(TestScript.SIDE_EFFECT);
    // then
    final Boolean wasExecuted = engine.executeScript(TestScript.CHECK_SIDE_EFFECT, Boolean.class);
    assertThat(wasExecuted).isTrue();
  }

  @Test
  @DisplayName("should call void JS function with provided arguments")
  void shouldCallVoidFunctionWithArguments() {
    // given
    engine.start();
    // when
    engine.callFunction("setCounter", 100);
    // then
    final Integer currentCounter = engine.callFunction("getCounter", Integer.class);
    assertThat(currentCounter).isEqualTo(100);
  }

  @Test
  @DisplayName("should correctly handle generic return type in callFunction")
  void shouldHandleGenericReturnTypeInCallFunction() {
    // given
    engine.start();
    // when
    final Map<String, Object> result =
        CastUtil.unsafeCast(engine.callFunction("createObject", Map.class, "John", 30));
    // then
    assertThat(result).containsEntry("name", "John").containsEntry("age", 30);
  }

  @Test
  @DisplayName("should throw IOException when script file is missing during executeScript")
  void shouldThrowIOExceptionWhenScriptIsMissing() {
    // given
    engine.start();
    final ScriptFile missingFile = () -> "non-existent-path.js";
    // when & then
    assertThatThrownBy(() -> engine.executeScript(missingFile))
        .isInstanceOf(java.io.IOException.class);
  }
}
