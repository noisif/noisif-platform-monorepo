/*
 * Copyright (c) 2022-2026 NOISIF. All Rights Reserved.
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
package xyz.noisif.nsl.netclient.rest.jetty.spec;

import org.eclipse.jetty.client.BytesRequestContent;
import org.eclipse.jetty.client.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.codec.serialization.MessageSerializer;
import xyz.noisif.nsl.common.reflect.LoadedViaReflection;
import xyz.noisif.nsl.net.http.header.CommonHttpHeaderName;
import xyz.noisif.nsl.netclient.rest.spec.GenericRequestSpec;
import xyz.noisif.nsl.netclient.rest.spec.HeaderConsumer;

@LoadedViaReflection
public class JettyRawBodyStrategy implements JettyBodyStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(JettyRawBodyStrategy.class);

  @Override
  public boolean supports(GenericRequestSpec spec) {
    return spec.getBody() != null
        && (spec.getFormParams() == null || spec.getFormParams().isEmpty());
  }

  @Override
  public Request.Content buildContent(
      GenericRequestSpec spec, MessageSerializer serializer, HeaderConsumer headerConsumer) {
    final String mimeType = serializer.getFormat().getMimeType();
    LOG.trace(
        "Building raw body, format: {}, mime type: {}",
        serializer.getFormat().getFormatName(),
        mimeType);
    if (mimeType != null) {
      headerConsumer.addHeader(CommonHttpHeaderName.CONTENT_TYPE, mimeType);
    }
    final byte[] serializedBytes = serializer.serializeToBytes(spec.getBody());
    return new BytesRequestContent(serializedBytes);
  }
}
