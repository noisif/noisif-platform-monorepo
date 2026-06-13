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
package xyz.noisif.nsl.http.exception.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.http.HttpRequest;
import xyz.noisif.nsl.http.HttpResponse;
import xyz.noisif.nsl.net.http.HttpStatus;

public class GlobalExceptionHandler implements ExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @Override
  public boolean supports(Throwable throwable) {
    return true;
  }

  @Override
  public void handle(HttpRequest req, HttpResponse res, Throwable throwable) {
    LOG.error("Internal server error at {}: ", req.getPath(), throwable);
    res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
    res.end();
  }
}
