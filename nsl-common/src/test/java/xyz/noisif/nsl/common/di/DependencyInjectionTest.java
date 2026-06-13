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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.noisif.nsl.common.reflect.ClassScanner;
import xyz.noisif.nsl.common.reflect.TypeReference;

import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

class DependencyInjectionTest {
  private ComponentProvider componentProvider;

  @BeforeEach
  void setUp() {
    // given
    final ClassScanner scanner = mock(ClassScanner.class);
    when(scanner.getTypesAnnotatedWith(Singleton.class))
        .thenReturn(
            Set.of(
                MarkedComponent.class,
                SimpleComponent.class,
                TestInterfaceComponent.class,
                SecondTestInterfaceComponent.class));
    final ApplicationContext context =
        ApplicationContext.createDefault(
            scanner, Map.of(ComponentProvider.class, GuiceComponentProvider.class));
    componentProvider = context.getComponentProvider();
  }

  @Test
  @DisplayName("should provide singleton instances for all @Injectable classes")
  void shouldProvideSingletonInjectables() {
    // when
    final MarkedComponent instance1 = componentProvider.getInstance(MarkedComponent.class);
    final MarkedComponent instance2 = componentProvider.getInstance(MarkedComponent.class);
    final SimpleComponent simple = componentProvider.getInstance(SimpleComponent.class);
    // then
    assertThat(instance1).isNotNull();
    assertThat(simple).isNotNull();
    assertThat(instance1).isSameAs(instance2); // singleton test
  }

  @Test
  @DisplayName("should find instances by custom annotation")
  void shouldFindInstancesByAnnotation() {
    // when
    Collection<Object> found = componentProvider.getInstancesAnnotatedWith(TestMarker.class);
    // then
    assertThat(found).hasSize(1);
    assertThat(found.iterator().next()).isInstanceOf(MarkedComponent.class);
  }

  @Test
  @DisplayName("should return empty collection when no components have the annotation")
  void shouldReturnEmptyForMissingAnnotation() {
    // when
    final Collection<Object> found = componentProvider.getInstancesAnnotatedWith(Override.class);
    // then
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("should find all instances by TypeReference")
  void shouldFindInstancesByTypeReference() {
    // given
    final TypeReference<TestInterface> pluginType = new TypeReference<>() {};
    // when
    final Collection<TestInterface> plugins = componentProvider.getInstancesOf(pluginType);
    // then
    assertThat(plugins)
        .hasSize(2)
        .hasOnlyElementsOfTypes(TestInterfaceComponent.class, SecondTestInterfaceComponent.class);
  }
}

@Singleton
@TestMarker
class MarkedComponent {}

@Singleton
class SimpleComponent {}

@Singleton
class TestInterfaceComponent implements TestInterface {}

@Singleton
class SecondTestInterfaceComponent implements TestInterface {}
