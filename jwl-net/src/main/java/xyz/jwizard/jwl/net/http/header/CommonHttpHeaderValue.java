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
package xyz.jwizard.jwl.net.http.header;

public enum CommonHttpHeaderValue implements HttpHeaderValue {
    APPLICATION_JSON_UTF_8("application/json; charset=utf-8"),
    TEXT_PLAIN_UTF_8("text/plain; charset=utf-8"),
    APPLICATION_JSON("application/json"),
    APPLICATION_X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),
    ;

    private final String code;

    CommonHttpHeaderValue(String code) {
        this.code = code;
    }

    @Override
    public String buildWithArgs(Object... args) {
        return String.format(code, args);
    }
}
