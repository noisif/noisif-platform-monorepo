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
package xyz.noisif.nsl.common.util.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.function.Function;

class JvmRuntimeUtilTest {
  @Test
  @DisplayName("should extract and parse value when JVM argument is present")
  void shouldExtractAndParseWhenArgIsPresent() {
    try (final MockedStatic<ManagementFactory> mockedFactory =
        Mockito.mockStatic(ManagementFactory.class)) {
      // given
      final RuntimeMXBean mockMxBean = Mockito.mock(RuntimeMXBean.class);
      Mockito.when(mockMxBean.getInputArguments())
          .thenReturn(List.of("-Xmx2G", "-XX:MaxDirectMemorySize=4M", "-Dmy.property=true"));
      mockedFactory.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockMxBean);
      final DefaultJvmArg arg = DefaultJvmArg.MAX_DIRECT_MEMORY_SIZE;
      // when
      final Long result = JvmRuntimeUtil.getJvmArg(arg);
      // then
      assertThat(result).isEqualTo(4194304L);
    }
  }

  @Test
  @DisplayName("should ignore partial prefix matches and find correct argument")
  void shouldIgnorePartialMatches() {
    try (final MockedStatic<ManagementFactory> mockedFactory =
        Mockito.mockStatic(ManagementFactory.class)) {
      // given
      final RuntimeMXBean mockMxBean = Mockito.mock(RuntimeMXBean.class);
      Mockito.when(mockMxBean.getInputArguments())
          .thenReturn(List.of("-XX:MaxDirectMemorySizeFake=10G", "-XX:MaxDirectMemorySize=1G"));
      mockedFactory.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockMxBean);
      // when
      final Long result = JvmRuntimeUtil.getJvmArg(DefaultJvmArg.MAX_DIRECT_MEMORY_SIZE);
      // then
      assertThat(result).isEqualTo(1073741824L); // 1GB
    }
  }

  @Test
  @DisplayName("should return default value when jvm argument is missing")
  void shouldReturnDefaultValueWhenArgMissing() {
    try (final MockedStatic<ManagementFactory> mockedFactory =
        Mockito.mockStatic(ManagementFactory.class)) {
      // then
      final RuntimeMXBean mockMxBean = Mockito.mock(RuntimeMXBean.class);
      Mockito.when(mockMxBean.getInputArguments()).thenReturn(List.of("-Xms1G"));
      mockedFactory.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockMxBean);
      // when
      final Long result = JvmRuntimeUtil.getJvmArg(DefaultJvmArg.MAX_DIRECT_MEMORY_SIZE, 999L);
      // then
      assertThat(result).isEqualTo(999L);
    }
  }

  @Test
  @DisplayName(
      "should safely return default value when JvmArg implementation is broken (returns nulls)")
  void shouldReturnDefaultWhenJvmArgIsBroken() {
    try (MockedStatic<ManagementFactory> mockedFactory =
        Mockito.mockStatic(ManagementFactory.class)) {
      // given
      final RuntimeMXBean mockMxBean = Mockito.mock(RuntimeMXBean.class);
      Mockito.when(mockMxBean.getInputArguments()).thenReturn(List.of("-XX:NonExistentFlag=boom"));
      mockedFactory.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockMxBean);
      final MissingJvmArg brokenArg = new MissingJvmArg();
      final Long defaultValue = 777L;
      // when
      final Long result = JvmRuntimeUtil.getJvmArg(brokenArg, defaultValue);
      // then
      assertThat(result).isEqualTo(defaultValue);
    }
  }
}

class MissingJvmArg implements JvmArg {
  @Override
  public String getPrefix() {
    return "-XX:NonExistentFlag=";
  }

  @Override
  public Function<String, ?> getTransformer() {
    return null;
  }

  @Override
  public Class<?> getType() {
    return null;
  }
}
