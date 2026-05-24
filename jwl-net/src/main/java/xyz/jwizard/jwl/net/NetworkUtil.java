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
package xyz.jwizard.jwl.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.jwizard.jwl.common.bootstrap.ForbiddenInstantiationException;
import xyz.jwizard.jwl.common.util.StringUtil;
import xyz.jwizard.jwl.common.util.io.IoUtil;

public class NetworkUtil {
    private static final String HTTP_SCHEME = "http://";
    private static final String HTTPS_SCHEME = "https://";

    private NetworkUtil() {
        throw new ForbiddenInstantiationException(NetworkUtil.class);
    }

    public static HostPort parseHostPort(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address string cannot be null or empty");
        }
        final List<String> parts = StringUtil.split(address, ':');
        if (parts.size() != 2) {
            throw new IllegalArgumentException("Invalid address format. Expected 'host:port', " +
                "but got: '" + address + "'");
        }
        try {
            final String host = parts.getFirst().trim();
            final int port = Integer.parseInt(parts.getLast().trim());
            return HostPort.from(host, port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number in address definition: '" +
                address + "'", e);
        }
    }

    public static URI parseToUri(NetworkProtocol networkProtocol, HostPort hostPort) {
        if (networkProtocol == null || hostPort == null) {
            return null;
        }
        final String scheme = networkProtocol.getScheme();
        final String host = hostPort.host();
        final int port = hostPort.port();
        try {
            return new URI(scheme, null, host, port, null, null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(
                "Cannot build valid URI for scheme '" + scheme + "' and address '" + host + ":" +
                    port + "'", ex
            );
        }
    }

    public static boolean isAbsoluteUrl(String uri) {
        return uri != null && (uri.startsWith(HTTP_SCHEME) || uri.startsWith(HTTPS_SCHEME));
    }

    public static String concatPaths(String baseUrl, String uriPath) {
        final String base = IoUtil.removeTrailingSlash(baseUrl);
        if (!uriPath.startsWith("/")) {
            uriPath = "/" + uriPath;
        }
        return base + uriPath;
    }

    public static String addQueryParameter(String originalUri, String key, String value) {
        if (originalUri == null) {
            return null;
        }
        if (key == null || value == null) {
            return originalUri;
        }
        String fragment = "";
        String uriWithoutFragment = originalUri;
        final int hashIdx = originalUri.indexOf('#');
        if (hashIdx != -1) {
            uriWithoutFragment = originalUri.substring(0, hashIdx);
            fragment = originalUri.substring(hashIdx);
        }
        final String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        final String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
        final String separator = uriWithoutFragment.contains("?") ? "&" : "?";
        return uriWithoutFragment + separator + encodedKey + "=" + encodedValue + fragment;
    }

    public static Map<String, String> getQueryParameters(String originalUri) {
        if (originalUri == null || originalUri.isEmpty()) {
            return Map.of();
        }
        try {
            final int hashIdx = originalUri.indexOf('#');
            final String uriToParse = (hashIdx != -1)
                ? originalUri.substring(0, hashIdx)
                : originalUri;
            final URI uri = new URI(uriToParse);
            final String query = uri.getQuery();
            if (query == null || query.isEmpty()) {
                return Map.of();
            }
            final Map<String, String> queryParams = new HashMap<>();
            final List<String> pairs = StringUtil.split(query, '&');
            for (final String pair : pairs) {
                final String[] keyValue = pair.split("=", 2);
                final String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                final String value = (keyValue.length > 1)
                    ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8)
                    : "";
                queryParams.put(key, value);
            }
            return queryParams;
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
