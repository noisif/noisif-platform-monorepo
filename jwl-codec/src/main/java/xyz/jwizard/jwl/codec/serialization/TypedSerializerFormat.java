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
package xyz.jwizard.jwl.codec.serialization;

import org.jspecify.annotations.NonNull;

import xyz.jwizard.jwl.codec.DataType;

public record TypedSerializerFormat(SerializerFormat baseFormat, DataType dataType)
    implements SerializerFormat {
  public static TypedSerializerFormat from(SerializerFormat format, DataType dataType) {
    return new TypedSerializerFormat(format, dataType);
  }

  @Override
  public String getFormatName() {
    return baseFormat.getFormatName() + "+" + dataType.getCode();
  }

  @Override
  public String getMimeType() {
    // application/x-jwl-[dataType]+[baseFormat]
    return "application/x-jwl-" + dataType.getCode() + "+" + baseFormat.getFormatName();
  }

  @Override
  @NonNull
  public String toString() {
    return getFormatName();
  }
}
