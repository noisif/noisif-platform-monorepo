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
