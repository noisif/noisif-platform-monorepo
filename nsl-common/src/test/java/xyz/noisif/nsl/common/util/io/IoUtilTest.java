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
package xyz.noisif.nsl.common.util.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

class IoUtilTest {
  @Test
  @DisplayName("should return valid URL for an existing resource")
  void shouldReturnUrlForExistingResource() throws IOException {
    // given
    final String existingFile = "test/test-resource.txt";
    // when
    final URL resourceUrl = IoUtil.getRequiredResourceUrl(existingFile);
    // then
    assertThat(resourceUrl).isNotNull();
    assertThat(resourceUrl.getPath()).endsWith(existingFile);
  }

  @Test
  @DisplayName("should throw IOException when URL resource does not exist")
  void shouldThrowExceptionWhenUrlResourceDoesNotExist() {
    // given
    final String nonExistingFile = "non-existing.txt";
    // when
    final Throwable thrown = catchThrowable(() -> IoUtil.getRequiredResourceUrl(nonExistingFile));
    // then
    assertThat(thrown)
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Unable to find file on classpath: " + nonExistingFile);
  }

  @Test
  @DisplayName("should return content string for an existing resource")
  void shouldReturnContentForExistingResource() throws IOException {
    // given
    final String existingFile = "test/test-resource.txt";
    // when
    final String content = IoUtil.getResourceAsString(existingFile);
    // then
    assertThat(content).isNotNull();
    assertThat(content.trim()).isEqualTo("This is test resource.");
  }

  @Test
  @DisplayName("should throw FileNotFoundException when string resource does not exist")
  void shouldThrowExceptionWhenStringResourceDoesNotExist() {
    // given
    final String nonExistingFile = "non-existing.txt";
    // when
    final Throwable thrown = catchThrowable(() -> IoUtil.getResourceAsString(nonExistingFile));
    // then
    assertThat(thrown)
        .isInstanceOf(FileNotFoundException.class)
        .hasMessageContaining("not found")
        .hasMessageContaining(nonExistingFile);
  }
}
