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
package xyz.jwizard.jwl.netclient.websocket;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.jwizard.jwl.net.NetworkUtil;
import xyz.jwizard.jwl.net.http.header.HttpHeaderName;

public class WsClientUpgradeRequest {
    private final Map<String, List<String>> headers = new HashMap<>();
    private String uri;

    public WsClientUpgradeRequest(URI uri) {
        this.uri = uri.toASCIIString();
    }

    public void setHeader(HttpHeaderName name, List<String> value) {
        headers.put(name.getCode(), value);
    }

    public void setHeader(HttpHeaderName name, String value) {
        setHeader(name, List.of(value));
    }

    public void addQueryParameter(String key, String value) {
        uri = NetworkUtil.addQueryParameter(uri, key, value);
    }

    public URI getUri() {
        return URI.create(uri);
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }
}
