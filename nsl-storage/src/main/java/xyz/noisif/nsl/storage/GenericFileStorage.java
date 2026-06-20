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

import xyz.noisif.nsl.common.bootstrap.lifecycle.IdempotentService;
import xyz.noisif.nsl.common.file.FileTypeRegistry;
import xyz.noisif.nsl.common.util.Assert;

public abstract class GenericFileStorage extends IdempotentService {
  protected final FileTypeRegistry fileTypeRegistry;

  protected FileStorageAction fileStorageAction;

  protected GenericFileStorage(AbstractBuilder<?> builder) {
    fileTypeRegistry = builder.fileTypeRegistry;
  }

  public FileStorageAction getFileStorageAction() {
    if (fileStorageAction == null) {
      throw new IllegalStateException("FileStorageAction is not initialized");
    }
    return fileStorageAction;
  }

  protected abstract static class AbstractBuilder<B extends AbstractBuilder<B>> {
    private FileTypeRegistry fileTypeRegistry;

    protected AbstractBuilder() {}

    public B fileTypeRegistry(FileTypeRegistry fileTypeRegistry) {
      this.fileTypeRegistry = fileTypeRegistry;
      return self();
    }

    protected void validate() {
      Assert.notNull(fileTypeRegistry, "fileTypeRegistry");
    }

    protected abstract B self();

    public abstract GenericFileStorage build();
  }
}
