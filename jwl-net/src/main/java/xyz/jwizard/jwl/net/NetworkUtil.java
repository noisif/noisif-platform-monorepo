/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jwizard.jwl.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import xyz.jwizard.jwl.common.bootstrap.ForbiddenInstantiationException;
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
        final String[] parts = address.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid address format. Expected 'host:port', " +
                "but got: '" + address + "'");
        }
        try {
            final String host = parts[0].trim();
            final int port = Integer.parseInt(parts[1].trim());
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
}
