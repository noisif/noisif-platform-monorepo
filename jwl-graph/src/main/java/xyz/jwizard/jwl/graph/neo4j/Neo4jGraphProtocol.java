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
package xyz.jwizard.jwl.graph.neo4j;

import xyz.jwizard.jwl.graph.GraphProtocol;

public enum Neo4jGraphProtocol implements GraphProtocol {
  BOLT("bolt", false, false),
  NEO4J("neo4j", false, false),
  NEO4J_S("neo4j+s", true, true), // TLS with trusted CA
  NEO4J_SSC("neo4j+ssc", true, false), // self-signed CA
  ;

  private final String scheme;
  private final boolean encrypted;
  private final boolean strictTlsValidation;

  Neo4jGraphProtocol(String scheme, boolean encrypted, boolean strictTlsValidation) {
    this.scheme = scheme;
    this.encrypted = encrypted;
    this.strictTlsValidation = strictTlsValidation;
  }

  @Override
  public String getScheme() {
    return scheme;
  }

  @Override
  public boolean isEncrypted() {
    return encrypted;
  }

  @Override
  public boolean requestStrictTlsValidation() {
    return strictTlsValidation;
  }
}
