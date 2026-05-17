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
package xyz.jwizard.jwl.sql.config;

import java.util.Map;

import xyz.jwizard.jwl.common.bootstrap.CriticalBootstrapException;
import xyz.jwizard.jwl.common.util.StringUtil;
import xyz.jwizard.jwl.net.HostPort;

public enum SqlDatabaseDialect {
    POSTGRESQL("jdbc:postgresql://%s:%d/%s", Map.of(
        "cachePrepStmts", "true",
        "prepStmtCacheSize", "250",
        "prepStmtCacheSqlLimit", "2048",
        "reWriteBatchedInserts", "true"
    )),
    ;

    private final String urlTemplate;
    private final Map<String, String> defaultDriverProperties;

    SqlDatabaseDialect(String urlTemplate, Map<String, String> defaultDriverProperties) {
        this.urlTemplate = urlTemplate;
        this.defaultDriverProperties = defaultDriverProperties;
    }

    public static SqlDatabaseDialect fromString(String dialectName) {
        try {
            return SqlDatabaseDialect.valueOf(StringUtil.toUpperCase(dialectName));
        } catch (IllegalArgumentException ex) {
            throw new CriticalBootstrapException("Unsupported database dialect: "
                + dialectName, ex);
        }
    }

    public String buildJdbcUrl(HostPort hostPort, String databaseName) {
        return String.format(urlTemplate, hostPort.host(), hostPort.port(), databaseName);
    }

    public Map<String, String> defaultDriverProperties() {
        return defaultDriverProperties;
    }
}
