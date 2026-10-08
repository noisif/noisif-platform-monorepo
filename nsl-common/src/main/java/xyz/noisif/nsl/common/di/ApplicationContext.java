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
package xyz.noisif.nsl.common.di;

import com.google.inject.Guice;
import com.google.inject.Injector;

import xyz.noisif.nsl.common.reflect.ClassScanner;

import java.util.Map;

public class ApplicationContext {
  private final ComponentProvider componentProvider;
  private final ClassScanner classScanner;

  private ApplicationContext(
      ClassScanner classScanner,
      Map<Class<?>, Class<?>> components,
      Map<Class<?>, Object> instanceComponents) {
    this.classScanner = classScanner;
    final Injector injector =
        Guice.createInjector(
            new ManualBootstrapModule(components, instanceComponents),
            new AutoScanModule(classScanner));
    componentProvider = injector.getInstance(ComponentProvider.class);
  }

  public static ApplicationContext create(
      ClassScanner classScanner,
      Map<Class<?>, Class<?>> components,
      Map<Class<?>, Object> instanceComponents) {
    return new ApplicationContext(classScanner, components, instanceComponents);
  }

  public static ApplicationContext createDefault(
      ClassScanner classScanner, Map<Class<?>, Class<?>> components) {
    return create(classScanner, components, Map.of());
  }

  public ComponentProvider getComponentProvider() {
    return componentProvider;
  }

  public ClassScanner getClassScanner() {
    return classScanner;
  }
}
