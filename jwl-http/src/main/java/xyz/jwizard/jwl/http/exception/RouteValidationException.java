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
package xyz.jwizard.jwl.http.exception;

import xyz.jwizard.jwl.common.bootstrap.CriticalBootstrapException;

import java.io.Serial;
import java.lang.reflect.Method;

public class RouteValidationException extends CriticalBootstrapException {
  @Serial private static final long serialVersionUID = 1L;

  public RouteValidationException(Method method, String details) {
    super(
        String.format(
            "Invalid route configuration in %s.%s(): %s",
            method.getDeclaringClass().getSimpleName(), method.getName(), details));
  }
}
