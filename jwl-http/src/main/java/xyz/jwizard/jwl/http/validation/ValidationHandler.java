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
