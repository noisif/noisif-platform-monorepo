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
package xyz.noisif.nsl.zcache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import xyz.noisif.nsl.zcache.lru.ConcurrentLruNativeStorage;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

class GenericNativeStorageBuilderTest {
  @Test
  @DisplayName("should configure max bytes from JVM MaxDirectMemorySize flag")
  void shouldConfigureFromJvmFlag() {
    try (final MockedStatic<ManagementFactory> mockedFactory =
        Mockito.mockStatic(ManagementFactory.class)) {
      // given
      final RuntimeMXBean mockMxBean = Mockito.mock(RuntimeMXBean.class);
      Mockito.when(mockMxBean.getInputArguments())
          .thenReturn(List.of("-XX:MaxDirectMemorySize=100M"));
      mockedFactory.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockMxBean);
      // when
      final ConcurrentLruNativeStorage storage =
          ConcurrentLruNativeStorage.builder().useJvmMaxDirectMemory().initialCapacity(128).build();
      // then: 100M = 104,857,600 bytes
      assertThat(storage).extracting("maxBytes").isEqualTo(104857600L);
      assertThat(storage)
          .extracting("memorySource")
          .isEqualTo("JVM flag (-XX:MaxDirectMemorySize)");
    }
  }

  @Test
  @DisplayName(
      "should configure max bytes from JVM MaxDirectMemorySize flag using a usage fraction")
  void shouldConfigureFromJvmFlagWithFraction() {
    try (final MockedStatic<ManagementFactory> mockedFactory =
        Mockito.mockStatic(ManagementFactory.class)) {
      // given
      final RuntimeMXBean mockMxBean = Mockito.mock(RuntimeMXBean.class);
      Mockito.when(mockMxBean.getInputArguments())
          .thenReturn(List.of("-XX:MaxDirectMemorySize=100M"));
      mockedFactory.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockMxBean);
      // when
      final ConcurrentLruNativeStorage storage =
          ConcurrentLruNativeStorage.builder()
              .useJvmMaxDirectMemory(0.25)
              .initialCapacity(128)
              .build();
      // then: 25% of 100M = 26,214,400 bytes
      assertThat(storage).extracting("maxBytes").isEqualTo(26214400L);
      assertThat(storage).extracting("memorySource").asString().contains("fraction: 0.25");
    }
  }
}
