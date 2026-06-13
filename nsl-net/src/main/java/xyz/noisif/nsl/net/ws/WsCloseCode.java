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
package xyz.noisif.nsl.net.ws;

import xyz.noisif.nsl.common.util.StringUtil;
import xyz.noisif.nsl.net.CloseCode;

public enum WsCloseCode implements CloseCode {
  NORMAL(1000, "Normal closure"),
  REPLACED_SESSION(1000, "Replaced by new session"),
  UNSUPPORTED_FRAME_TYPE(1003, "Unsupported frame type"),
  INTERNAL_SERVER_ERROR(1011, "Internal server error"),
  SERVER_OVERLOADED(1011, "Server overloaded"),
  ;

  private static final int MAX_CLOSE_REASON_BYTES = 123;

  private final int code;
  private final String reason;

  WsCloseCode(int code, String reason) {
    this.code = code;
    this.reason = StringUtil.truncateToUtf8Bytes(reason, MAX_CLOSE_REASON_BYTES);
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  public String getDefaultReason() {
    return reason;
  }
}
