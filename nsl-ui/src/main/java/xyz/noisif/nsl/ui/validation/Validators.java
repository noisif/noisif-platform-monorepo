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
package xyz.noisif.nsl.ui.validation;

import xyz.noisif.nsl.common.bootstrap.ForbiddenInstantiationException;

import java.util.Optional;
import java.util.regex.Pattern;

public class Validators {
  private Validators() {
    throw new ForbiddenInstantiationException(Validators.class);
  }

  public static ValidationRule required(String errorMessage) {
    return value ->
        (value == null || value.isBlank()) ? Optional.of(errorMessage) : Optional.empty();
  }

  public static ValidationRule minLength(int min, String errorMessage) {
    return value ->
        (value == null || value.length() < min) ? Optional.of(errorMessage) : Optional.empty();
  }

  public static ValidationRule maxLength(int max, String errorMessage) {
    return value ->
        (value != null && value.length() > max) ? Optional.of(errorMessage) : Optional.empty();
  }

  public static ValidationRule regex(String pattern, String errorMessage) {
    final Pattern compiled = Pattern.compile(pattern);
    return value ->
        (value == null || !compiled.matcher(value).matches())
            ? Optional.of(errorMessage)
            : Optional.empty();
  }
}
