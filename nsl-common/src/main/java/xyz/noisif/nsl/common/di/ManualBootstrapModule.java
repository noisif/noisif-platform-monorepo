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

import com.google.inject.AbstractModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.util.CastUtil;

import java.util.Map;

class ManualBootstrapModule extends AbstractModule {
  private static final Logger LOG = LoggerFactory.getLogger(ManualBootstrapModule.class);

  private final Map<Class<?>, Class<?>> components;
  private final Map<Class<?>, Object> instanceComponents;

  ManualBootstrapModule(
      Map<Class<?>, Class<?>> components, Map<Class<?>, Object> instanceComponents) {
    this.components = components;
    this.instanceComponents = instanceComponents;
  }

  @Override
  protected void configure() {
    LOG.debug("Configuring bootstrap module");
    for (Map.Entry<Class<?>, Class<?>> entry : components.entrySet()) {
      final Class<Object> interfaceClass = CastUtil.unsafeCast(entry.getKey());
      final Class<Object> implementationClass = CastUtil.unsafeCast(entry.getValue());
      LOG.debug(
          "Binding infrastructure class: {} -> {}",
          interfaceClass.getSimpleName(),
          implementationClass.getSimpleName());
      bind(interfaceClass).to(implementationClass).asEagerSingleton();
    }
    for (Map.Entry<Class<?>, Object> entry : instanceComponents.entrySet()) {
      final Class<Object> type = CastUtil.unsafeCast(entry.getKey());
      final Object instance = entry.getValue();
      LOG.debug(
          "Binding existing instance: {} -> instance of {}",
          type.getSimpleName(),
          instance.getClass().getSimpleName());
      bind(type).toInstance(instance);
    }
  }
}
