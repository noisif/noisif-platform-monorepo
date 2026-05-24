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
