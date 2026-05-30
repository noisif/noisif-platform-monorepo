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
package xyz.jwizard.jwl.codec.envelope;

public enum StandardOpCode implements OpCode {
  RATE_LIMIT_EXCEEDED(0x01, 0x01), // 65537 (0x010001)
  UNKNOWN_ACTION(0x01, 0x02), // 65538 (0x010002)
  INVALID_PAYLOAD(0x01, 0x03), // 65539 (0x010003)
  INTERNAL_ERROR(0x01, 0x04), // 65540 (0x010004)
  HEARTBEAT(0x01, 0x05), // 65541 (0x010005)
  ;

  private final int code;

  StandardOpCode(int category, int action) {
    code = OpCode.combine(category, action);
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  public String getName() {
    return name();
  }

  @Override
  public String toString() {
    return asString();
  }
}
