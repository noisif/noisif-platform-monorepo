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
package xyz.jwizard.jwl.http.filter;

import xyz.jwizard.jwl.http.HttpRequest;
import xyz.jwizard.jwl.http.HttpResponse;
import xyz.jwizard.jwl.http.header.TestHttpHeaderName;
import xyz.jwizard.jwl.http.header.TestHttpHeaderValue;
import xyz.jwizard.jwl.http.route.Route;
import xyz.jwizard.jwl.net.http.HttpStatus;

import jakarta.inject.Singleton;

@Singleton
public class DummySecurityFilter implements HttpFilter {
    @Override
    public boolean supports(Route route) {
        return true;
    }

    @Override
    public boolean preHandle(HttpRequest req, HttpResponse res) {
        res.setHeader(TestHttpHeaderName.X_TEST_FILTER, TestHttpHeaderValue.EXECUTED);
        if ("/api/blocked".equals(req.getPath())) {
            res.setStatus(HttpStatus.FORBIDDEN_403);
            return false;
        }
        return true;
    }
}
