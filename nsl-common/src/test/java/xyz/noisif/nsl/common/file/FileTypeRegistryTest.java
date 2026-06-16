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
package xyz.noisif.nsl.common.file;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.noisif.nsl.common.bootstrap.CriticalBootstrapException;

import java.util.List;

class FileTypeRegistryTest {
  private FileTypeRegistry registry;

  @BeforeEach
  void setUp() {
    registry = FileTypeRegistry.createWithOverwrite(StandardFileType.RAW);
  }

  @Test
  @DisplayName("should throw exception when created with null fallback type")
  void shouldThrowExceptionWhenFallbackIsNull() {
    assertThrows(
        CriticalBootstrapException.class,
        () -> FileTypeRegistry.createWithOverwrite(null),
        "Should throw CriticalBootstrapException when fallback type is null");
  }

  @Test
  @DisplayName("should return fallback type for null or blank mime types")
  void shouldReturnFallbackForNullOrBlankMimeType() {
    assertSame(StandardFileType.RAW, registry.fromMimeType(null));
    assertSame(StandardFileType.RAW, registry.fromMimeType("   "));
  }

  @Test
  @DisplayName("should clean mime type by removing parameters and ignoring case")
  void shouldCleanMimeTypeAndResolveCorrectly() {
    // given
    final FileType htmlType = new TestFileType("text/html", "html");
    registry.register(htmlType);
    final String dirtyMimeType = "TEXT/HTML; charset=UTF-8";
    // when
    final FileType resolved = registry.fromMimeType(dirtyMimeType);
    // then
    assertSame(htmlType, resolved, "Registry should strip parameters and normalize case");
  }

  @Test
  @DisplayName("should throw exception on overwrite attempt when not allowed")
  void shouldThrowExceptionOnOverwriteWhenNotAllowed() {
    // given
    final FileTypeRegistry strictRegistry =
        FileTypeRegistry.createWithoutOverwrite(StandardFileType.RAW);
    final FileType type = new TestFileType("application/json", "json");
    strictRegistry.register(type);
    // when & then
    assertThrows(
        IllegalStateException.class,
        () -> strictRegistry.register(type),
        "Should forbid overwriting in strict mode");
  }

  @Test
  @DisplayName("should successfully register types from collections and varargs")
  void shouldRegisterMultipleTypesFromCollectionAndVarargs() {
    final FileType typeA = new TestFileType("audio/ogg", "ogg");
    final FileType typeB = new TestFileType("audio/mp3", "mp3");
    final FileType typeC = new TestFileType("video/mp4", "mp4");
    // when
    registry.registerTypes(List.of(typeA, typeB));
    registry.registerTypes(typeC);
    // then
    assertEquals(typeA, registry.fromMimeType("audio/ogg"), "Type A should be resolved correctly");
    assertEquals(typeB, registry.fromMimeType("audio/mp3"), "Type B should be resolved correctly");
    assertEquals(typeC, registry.fromMimeType("video/mp4"), "Type C should be resolved correctly");
  }

  @Test
  @DisplayName("should normalize input mime type case and parameters")
  void shouldNormalizeMimeType() {
    // given
    registry.registerTypes(StandardFileType.values());
    // when
    assertAll(
        "Normalization checks",
        () -> assertEquals(StandardFileType.JPEG, registry.fromMimeType("IMAGE/JPEG")),
        () -> assertEquals(StandardFileType.MP4, registry.fromMimeType("video/mp4; codecs=avc1")),
        () -> assertEquals(StandardFileType.AVIF, registry.fromMimeType("  image/avif  ")));
  }

  @Test
  @DisplayName("should return RAW when unknown mime type provided")
  void shouldReturnRawForUnknownType() {
    // given
    registry.register(StandardFileType.PNG);
    // when
    final FileType result = registry.fromMimeType("application/x-zip-compressed");
    // then
    assertEquals(StandardFileType.RAW, result, "Should return RAW for unknown MIME type");
  }

  @Test
  @DisplayName("should safely clean mime types even when malformed or missing separators")
  void shouldCleanMimeTypeSafely() {
    // given
    registry.register(StandardFileType.PNG);
    // when & then
    assertAll(
        "MIME normalization variants",
        () ->
            assertEquals(
                "image/png",
                registry.fromMimeType("image/png;charset=UTF-8").getMimeType(),
                "Should strip parameters after semicolon"),
        () ->
            assertEquals(
                "image/png",
                registry.fromMimeType("image/png").getMimeType(),
                "Should work correctly without semicolon"),
        () ->
            assertEquals(
                "image/png",
                registry.fromMimeType("  IMAGE/PNG  ").getMimeType(),
                "Should trim whitespaces and lower case"),
        () ->
            assertEquals(
                "image/png",
                registry.fromMimeType("  IMAGE/PNG;  ").getMimeType(),
                "Should handle whitespace around semicolon"));
  }
}

record TestFileType(String mimeType, String extension) implements FileType {
  @Override
  public String getMimeType() {
    return mimeType;
  }

  @Override
  public String getExt() {
    return extension;
  }
}
