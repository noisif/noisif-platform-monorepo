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
package xyz.jwizard.jwl.http.jetty.adapter;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import xyz.jwizard.jwl.http.HttpResponse;
import xyz.jwizard.jwl.net.http.HttpStatus;
import xyz.jwizard.jwl.net.http.header.HttpHeaderName;
import xyz.jwizard.jwl.net.http.header.HttpHeaderValue;

public class JettyHttpResponseAdapter implements HttpResponse {
    private final Response response;
    private final Callback callback;

    public JettyHttpResponseAdapter(Response response, Callback callback) {
        this.response = response;
        this.callback = callback;
    }

    @Override
    public String getHeaderUnsafe(String name) {
        return response.getHeaders().get(name);
    }

    @Override
    public String getHeader(HttpHeaderName name) {
        return getHeaderUnsafe(name.getCode());
    }

    @Override
    public void setStatus(HttpStatus statusCode) {
        response.setStatus(statusCode.getCode());
    }

    @Override
    public void setHeader(HttpHeaderName name, HttpHeaderValue value, Object... args) {
        setHeaderUnsafe(name.getCode(), value.buildWithArgs(args));
    }

    @Override
    public void setHeader(HttpHeaderName name, String value) {
        setHeaderUnsafe(name.getCode(), value);
    }

    @Override
    public void setHeaderUnsafe(String name, String value) {
        // put means override, add create new header with same key
        response.getHeaders().put(name, value);
    }

    @Override
    public void write(String body, boolean last) {
        Content.Sink.write(response, last, body, callback);
    }

    @Override
    public void writeEmpty(boolean last) {
        write("", last);
    }

    @Override
    public void end() {
        callback.succeeded();
    }
}
