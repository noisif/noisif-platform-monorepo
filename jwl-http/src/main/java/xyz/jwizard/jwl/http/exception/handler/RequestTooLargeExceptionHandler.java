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
package xyz.jwizard.jwl.http.exception.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.http.HttpRequest;
import xyz.jwizard.jwl.http.HttpResponse;
import xyz.jwizard.jwl.http.exception.RequestTooLargeException;
import xyz.jwizard.jwl.net.http.HttpStatus;

public class RequestTooLargeExceptionHandler implements ExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RequestTooLargeExceptionHandler.class);

    @Override
    public boolean supports(Throwable throwable) {
        return throwable instanceof RequestTooLargeException;
    }

    @Override
    public void handle(HttpRequest req, HttpResponse res, Throwable throwable) {
        LOG.warn("Request too large [{}]: {}", req.getPath(), throwable.getMessage());
        res.setStatus(HttpStatus.PAYLOAD_TOO_LARGE_413);
        res.end();
    }
}
