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
package xyz.jwizard.jwl.netclient.rest.jetty.spec;

import org.eclipse.jetty.client.FormRequestContent;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.util.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.serialization.MessageSerializer;
import xyz.jwizard.jwl.common.reflect.LoadedViaReflection;
import xyz.jwizard.jwl.netclient.rest.spec.GenericRequestSpec;
import xyz.jwizard.jwl.netclient.rest.spec.HeaderConsumer;

import java.util.Map;

@LoadedViaReflection
public class JettyFormBodyStrategy implements JettyBodyStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(JettyFormBodyStrategy.class);

  @Override
  public boolean supports(GenericRequestSpec spec) {
    return spec.getFormParams() != null && !spec.getFormParams().isEmpty();
  }

  @Override
  public Request.Content buildContent(
      GenericRequestSpec spec, MessageSerializer serializer, HeaderConsumer headerConsumer) {
    LOG.trace("Building form-urlencoded body for request: {}", spec.getUrl());
    if (spec.getBody() != null) {
      throw new IllegalStateException(
          "Cannot use both form parameters and raw body in " + "the same request");
    }
    final Fields fields = new Fields();
    for (final Map.Entry<String, String> entry : spec.getFormParams().entrySet()) {
      fields.put(entry.getKey(), entry.getValue());
    }
    return new FormRequestContent(fields);
  }
}
