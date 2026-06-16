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
package xyz.noisif.nsl.storage;

import xyz.noisif.nsl.common.file.FileType;

import java.io.InputStream;
import java.util.List;

public interface FileStorageAction {
  void save(StorageNamespace namespace, String path, byte[] content, FileType fileType);

  FileContent<byte[]> get(StorageNamespace namespace, String path);

  FileContent<InputStream> getAsInputStream(StorageNamespace namespace, String path);

  void delete(StorageNamespace namespace, String path);

  boolean exists(StorageNamespace namespace, String path);

  void copy(StorageNamespace namespace, String sourcePath, String targetPath);

  void move(StorageNamespace namespace, String sourcePath, String targetPath);

  List<String> listFiles(StorageNamespace namespace, String prefix);

  default void save(
      StorageNamespace namespace,
      StoragePath path,
      byte[] content,
      FileType fileType,
      Object... pathArgs) {
    save(namespace, path.buildWithArgs(pathArgs), content, fileType);
  }

  default FileContent<byte[]> get(
      StorageNamespace namespace, StoragePath path, Object... pathArgs) {
    return get(namespace, path.buildWithArgs(pathArgs));
  }

  default FileContent<InputStream> getAsInputStream(
      StorageNamespace namespace, StoragePath path, Object... pathArgs) {
    return getAsInputStream(namespace, path.buildWithArgs(pathArgs));
  }

  default void delete(StorageNamespace namespace, StoragePath path, Object... pathArgs) {
    delete(namespace, path.buildWithArgs(pathArgs));
  }

  default boolean exists(StorageNamespace namespace, StoragePath path, Object... pathArgs) {
    return exists(namespace, path.buildWithArgs(pathArgs));
  }

  default void copy(
      StorageNamespace namespace, ResolvablePath sourcePath, ResolvablePath targetPath) {
    copy(namespace, sourcePath.buildFromArgs(), targetPath.buildFromArgs());
  }

  default void move(
      StorageNamespace namespace, ResolvablePath sourcePath, ResolvablePath targetPath) {
    move(namespace, sourcePath.buildFromArgs(), targetPath.buildFromArgs());
  }
}
