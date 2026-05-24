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
package xyz.jwizard.jwl.common.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.stream.Collectors;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import xyz.jwizard.jwl.common.reflect.TypeReference;
import xyz.jwizard.jwl.common.util.CastUtil;

import jakarta.inject.Inject;

public class GuiceComponentProvider implements ComponentProvider {
    private final Injector injector;

    @Inject
    public GuiceComponentProvider(Injector injector) {
        this.injector = injector;
    }

    @Override
    public <T> T getInstance(Class<T> clazz) {
        return injector.getInstance(clazz);
    }

    @Override
    public Collection<Object> getInstancesAnnotatedWith(Class<? extends Annotation> annotation) {
        return injector.getAllBindings().keySet().stream()
            .map(Key::getTypeLiteral)
            .map(TypeLiteral::getRawType)
            .filter(clazz -> clazz.isAnnotationPresent(annotation))
            .map(injector::getInstance)
            .collect(Collectors.toSet());
    }

    @Override
    public <T> Collection<T> getInstancesOf(Class<T> type) {
        return injector.getAllBindings().keySet().stream()
            .filter(key -> {
                final Class<?> boundRawType = key.getTypeLiteral().getRawType();
                return type.isAssignableFrom(boundRawType)
                    && !boundRawType.isInterface()
                    && !Modifier.isAbstract(boundRawType.getModifiers());
            })
            .map(key -> CastUtil.<T>unsafeCast(injector.getInstance(key)))
            .collect(Collectors.toSet());
    }

    @Override
    public <T> Collection<T> getInstancesOf(TypeReference<T> typeReference) {
        final TypeLiteral<T> guiceTypeLiteral = CastUtil.unsafeCast(TypeLiteral
            .get(typeReference.getType()));
        final Class<T> rawType = CastUtil.unsafeCast(guiceTypeLiteral.getRawType());
        return getInstancesOf(rawType);
    }
}
