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
package xyz.noisif.nsl.codec.envelope;

public record MessageEnvelope<T>(int op, T data) {
  public static <T> MessageEnvelope<T> of(OpCode opCode) {
    return new MessageEnvelope<>(opCode.getCode(), null);
  }

  public boolean isCategory(int category) {
    return ((op >> 16) & 0xFF) == category;
  }

  public int category() {
    return (op >> 16) & 0xFF;
  }

  public int actionId() {
    return op & 0xFF;
  }
}
