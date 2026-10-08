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
package xyz.noisif.nss.gateway.ws;

import xyz.noisif.nsl.codec.envelope.OpCode;

public enum GatewayOpCode implements OpCode {
  SUBSCRIBE_JOB(0x02, 0x01), // 131073
  SUBSCRIBE_JOB_ACK(0x02, 0x02), // 131074
  UNSUBSCRIBE_JOB(0x02, 0x03), // 131075
  UNSUBSCRIBE_JOB_ACK(0x02, 0x04), // 131076

  AUDIO_TRANSCODE_PROGRESS(0x03, 0x01), // 196609
  ;

  private final int code;

  GatewayOpCode(int category, int action) {
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
