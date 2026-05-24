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

import java.io.InputStream;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import xyz.jwizard.jwl.common.util.StringUtil;
import xyz.jwizard.jwl.http.HttpRequest;
import xyz.jwizard.jwl.net.http.header.CommonHttpHeaderName;
import xyz.jwizard.jwl.net.http.header.HttpHeaderName;

public class JettyHttpRequestAdapter implements HttpRequest {
    private final Request request;
    private MultiMap<String> queryParams;

    public JettyHttpRequestAdapter(Request request) {
        this.request = request;
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getPath() {
        return request.getHttpURI().getPath();
    }

    @Override
    public long getLength() {
        return request.getLength();
    }

    @Override
    public InputStream getInputStream() {
        return Content.Source.asInputStream(request);
    }

    @Override
    public String getQuery() {
        return request.getHttpURI().getQuery();
    }

    @Override
    public String getQueryParam(String name) {
        if (queryParams == null) {
            final String query = getQuery();
            if (query != null && !query.isEmpty()) {
                queryParams = UrlEncoded.decodeQuery(query);
            } else {
                queryParams = new MultiMap<>();
            }
        }
        return queryParams.getValue(name);
    }

    @Override
    public String getHeader(HttpHeaderName name) {
        return getHeaderUnsafe(name.getCode());
    }

    @Override
    public String getHeaderUnsafe(String name) {
        return request.getHeaders().get(name);
    }

    @Override
    public String getContentType() {
        final String contentType = getHeader(CommonHttpHeaderName.CONTENT_TYPE);
        if (contentType == null) {
            return null;
        }
        final int semicolonIndex = contentType.indexOf(';');
        final String rawType = (semicolonIndex != -1)
            ? contentType.substring(0, semicolonIndex)
            : contentType;
        return StringUtil.toLowerCase(rawType.trim());
    }
}
