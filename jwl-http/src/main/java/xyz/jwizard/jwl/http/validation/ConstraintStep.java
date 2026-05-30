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
package xyz.jwizard.jwl.http.validation;

import xyz.jwizard.jwl.common.util.CastUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

class ConstraintStep implements ValidationStep {
  private final Field field;
  private final Annotation annotation;
  private final AnnotationValidator<Annotation> validator;

  ConstraintStep(Field field, Annotation annotation, AnnotationValidator<?> validator) {
    this.field = field;
    this.annotation = annotation;
    this.validator = CastUtil.unsafeCast(validator);
  }

  @Override
  public void execute(Object target) throws IllegalAccessException {
    final Object value = field.get(target);
    validator.validate(annotation, field, value);
  }

  @Override
  public Field getField() {
    return field;
  }
}
