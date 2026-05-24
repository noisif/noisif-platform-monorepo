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

import java.lang.reflect.Method;

import com.google.inject.Injector;
import com.google.inject.Provider;

import xyz.jwizard.jwl.common.bootstrap.CriticalBootstrapException;
import xyz.jwizard.jwl.common.util.CastUtil;

class BeanProvider<T> implements Provider<T> {
    private final Class<?> configClass;
    private final Method method;
    private final Provider<Injector> injectorProvider;

    BeanProvider(Class<?> configClass, Method method, Provider<Injector> injectorProvider) {
        this.configClass = configClass;
        this.method = method;
        this.injectorProvider = injectorProvider;
        this.method.setAccessible(true);
    }

    @Override
    public T get() {
        try {
            final Injector injector = injectorProvider.get();
            final Object configInstance = injector.getInstance(configClass);
            final Class<?>[] paramTypes = method.getParameterTypes();
            final Object[] args = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                args[i] = injector.getInstance(paramTypes[i]);
            }
            return CastUtil.unsafeCast(method.invoke(configInstance, args));
        } catch (Exception ex) {
            throw new CriticalBootstrapException(
                "Failed to instantiate @Bean method: " + method.getName() + " in " +
                    configClass.getSimpleName(), ex
            );
        }
    }
}
