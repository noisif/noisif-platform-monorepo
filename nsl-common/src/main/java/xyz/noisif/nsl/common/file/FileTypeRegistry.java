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

import xyz.noisif.nsl.common.registry.GenericConcurrentRegistry;
import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.common.util.StringUtil;

import java.util.Arrays;
import java.util.Collection;

public class FileTypeRegistry extends GenericConcurrentRegistry<String, FileType> {
  private final FileType fallbackType;

  private FileTypeRegistry(boolean allowOverwrite, FileType fallbackType) {
    super(allowOverwrite);
    Assert.notNull(fallbackType, "fallbackType");
    this.fallbackType = fallbackType;
  }

  public static FileTypeRegistry createWithOverwrite(FileType fallbackType) {
    return new FileTypeRegistry(true, fallbackType);
  }

  public static FileTypeRegistry createWithOverwrite() {
    return new FileTypeRegistry(true, StandardFileType.RAW);
  }

  public static FileTypeRegistry createWithoutOverwrite(FileType fallbackType) {
    return new FileTypeRegistry(false, fallbackType);
  }

  public static FileTypeRegistry createWithoutOverwrite() {
    return new FileTypeRegistry(false, StandardFileType.RAW);
  }

  public FileTypeRegistry register(FileType fileType) {
    final String cleanKey = cleanMimeType(fileType.getMimeType());
    super.register(cleanKey, fileType);
    return this;
  }

  public FileTypeRegistry registerTypes(Collection<FileType> fileTypes) {
    if (fileTypes != null) {
      fileTypes.forEach(this::register);
    }
    return this;
  }

  public FileTypeRegistry registerTypes(FileType... fileTypes) {
    return registerTypes(Arrays.stream(fileTypes).toList());
  }

  public FileTypeRegistry withDefaults() {
    return registerTypes(StandardFileType.values());
  }

  public FileType fromMimeType(String mimeType) {
    if (mimeType == null || mimeType.isBlank()) {
      return fallbackType;
    }
    final String cleanKey = cleanMimeType(mimeType);
    final FileType resolvedType = getOrNull(cleanKey);
    return resolvedType != null ? resolvedType : fallbackType;
  }

  private String cleanMimeType(String rawMimeType) {
    final String cleanValue = StringUtil.splitAndGetFirst(rawMimeType, ';');
    final String valueToClean = (cleanValue != null) ? cleanValue : rawMimeType;
    return StringUtil.toLowerCase(valueToClean.trim());
  }
}
