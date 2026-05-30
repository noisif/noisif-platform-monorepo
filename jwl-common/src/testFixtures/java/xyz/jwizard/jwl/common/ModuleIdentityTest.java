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
package xyz.jwizard.jwl.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ModuleIdentityTest {
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Test
  void shouldIdentifyModule() {
    // given
    final String moduleName = getPackageSuffix() + "-" + getModuleName();
    final String packageName = getClass().getPackageName();
    // when
    log.info("running smoke test for module: {} (package: {})", moduleName, packageName);
    // then
    assertThat(moduleName).isNotBlank();
    assertThat(packageName).startsWith("xyz.jwizard." + getPackageSuffix());
  }

  protected abstract String getModuleName();

  protected abstract String getPackageSuffix();
}
