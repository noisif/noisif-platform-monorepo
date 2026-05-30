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
package xyz.jwizard.jwl.common.reflect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

interface BaseTestInterface {}

interface SubInterface extends BaseTestInterface {}

class ClassGraphScannerTest {
  @Test
  @DisplayName("should find only classes annotated with @TestComponent")
  void shouldFindAnnotatedClasses() {
    // given
    final String currentPackage = this.getClass().getPackageName();
    // when
    try (final ClassGraphScanner scanner = new ClassGraphScanner(currentPackage)) {
      final Set<Class<?>> foundClasses = scanner.getTypesAnnotatedWith(TestComponent.class);
      // then
      assertThat(foundClasses)
          .hasSizeGreaterThanOrEqualTo(2)
          .contains(ValidComponentOne.class, ValidComponentTwo.class)
          .doesNotContain(IgnoredComponent.class);
    }
  }

  @Test
  @DisplayName("should return an empty set when the package does not exist")
  void shouldReturnEmptySetForNonExistentPackage() {
    // given
    final String nonExistentPackage = "xyz.jwizard.jwl.this.package.does.not.exist";
    // when
    try (final ClassGraphScanner scanner = new ClassGraphScanner(nonExistentPackage)) {
      final Set<Class<?>> foundClasses = scanner.getTypesAnnotatedWith(TestComponent.class);
      // then
      assertThat(foundClasses).isEmpty();
    }
  }

  @Test
  @DisplayName("should find all subtypes (including abstract and interfaces)")
  void shouldFindAllSubtypes() {
    // given
    final String currentPackage = this.getClass().getPackageName();
    // when
    try (final ClassGraphScanner scanner = new ClassGraphScanner(currentPackage)) {
      final Set<Class<? extends BaseTestInterface>> foundClasses =
          scanner.getSubtypesOf(BaseTestInterface.class);
      // then
      assertThat(foundClasses)
          .contains(AbstractTestImpl.class, ConcreteTestImpl.class, SubInterface.class);
    }
  }

  @Test
  @DisplayName("should find only instantiable (concrete) subtypes")
  void shouldFindOnlyInstantiableSubtypes() {
    // given
    final String currentPackage = this.getClass().getPackageName();
    // when
    try (final ClassGraphScanner scanner = new ClassGraphScanner(currentPackage)) {
      final Set<Class<? extends BaseTestInterface>> foundClasses =
          scanner.getInstantiableSubtypesOf(BaseTestInterface.class);
      // then
      assertThat(foundClasses)
          .containsExactly(ConcreteTestImpl.class)
          .doesNotContain(AbstractTestImpl.class, SubInterface.class, BaseTestInterface.class);
    }
  }
}

@TestComponent
class ValidComponentOne {}

@TestComponent
class ValidComponentTwo {}

// class without the annotation - should not be found
class IgnoredComponent {}

abstract class AbstractTestImpl implements BaseTestInterface {}

class ConcreteTestImpl extends AbstractTestImpl {}
