/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jwizard.jwl.http.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import xyz.jwizard.jwl.common.util.CastUtil;

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
