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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.cache.ProviderCache;
import xyz.jwizard.jwl.http.annotation.validation.NestedValid;

public class ValidationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ValidationHandler.class);

    private final Map<Class<?>, List<ValidationStep>> classPlanCache = new ConcurrentHashMap<>();
    private final ProviderCache<Class<? extends Annotation>,
        Class<? extends Annotation>, AnnotationValidator<?>> validatorCache;

    public ValidationHandler(Set<AnnotationValidator<?>> validators) {
        validatorCache = new ProviderCache<>(validators, AnnotationValidator::supports);
        LOG.info("Initialized ValidatorHandler with {} validator(s)", validators.size());
    }

    public void validate(Object target) {
        if (target == null) {
            return;
        }
        final Class<?> clazz = target.getClass();
        final List<ValidationStep> steps = classPlanCache.computeIfAbsent(clazz,
            this::buildValidationPlan);
        if (!steps.isEmpty()) {
            LOG.debug("Executing {} validation step(s) for object of class: {}", steps.size(),
                clazz.getSimpleName());
        }
        for (final ValidationStep step : steps) {
            try {
                step.execute(target);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Security restriction: cannot access field '"
                    + step.getField().getName() + "' in class " + target.getClass().getName(), ex);
            }
        }
    }

    private List<ValidationStep> buildValidationPlan(Class<?> clazz) {
        LOG.debug("Cache miss, building validation plan for class: {}", clazz.getName());
        final List<ValidationStep> steps = new ArrayList<>();
        for (final Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(NestedValid.class)) {
                LOG.debug("Added nested validation step for field: {}", field.getName());
                steps.add(new NestedStep(field, this));
            }
            for (final Annotation annotation : field.getAnnotations()) {
                final Class<? extends Annotation> annType = annotation.annotationType();
                final AnnotationValidator<?> validator = validatorCache.get(annType, annType);
                if (validator != null) {
                    LOG.debug("Mapped field '{}' to validator: {}", field.getName(),
                        validator.getClass().getSimpleName());
                    steps.add(new ConstraintStep(field, annotation, validator));
                }
            }
        }
        LOG.debug("Validation plan built successfully, cached {} step(s) for class: {}",
            steps.size(), clazz.getSimpleName());
        return steps;
    }
}
