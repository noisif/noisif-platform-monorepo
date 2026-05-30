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
package xyz.jwizard.jwl.http.resolver.body;

import xyz.jwizard.jwl.codec.serialization.MessageSerializer;
import xyz.jwizard.jwl.codec.serialization.SerializerRegistry;
import xyz.jwizard.jwl.http.HttpRequest;
import xyz.jwizard.jwl.http.annotation.Body;
import xyz.jwizard.jwl.http.exception.RequestTooLargeException;
import xyz.jwizard.jwl.http.exception.RouteValidationException;
import xyz.jwizard.jwl.http.resolver.ArgumentResolver;
import xyz.jwizard.jwl.http.route.MatchResult;
import xyz.jwizard.jwl.http.validation.ValidationHandler;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

public class RequestBodyResolver implements ArgumentResolver {
  private final SerializerRegistry<MessageSerializer> serializerRegistry;
  private final ValidationHandler validationHandler;

  public RequestBodyResolver(
      SerializerRegistry<MessageSerializer> serializerRegistry,
      ValidationHandler validationHandler) {
    this.serializerRegistry = serializerRegistry;
    this.validationHandler = validationHandler;
  }

  @Override
  public boolean supports(Parameter parameter) {
    return parameter.isAnnotationPresent(Body.class);
  }

  @Override
  public Object resolve(Parameter parameter, HttpRequest req, MatchResult match) throws Exception {
    final long contentLength = req.getLength();
    if (contentLength == 0) {
      return null;
    }
    final BodyMediaSerializer mapping =
        BodyMediaSerializer.resolve(parameter.getType(), req.getContentType());

    final Body annotation = parameter.getAnnotation(Body.class);
    final long limit =
        (annotation != null && annotation.limit() > 0)
            ? annotation.unit().toBytes(annotation.limit())
            : mapping.getMaxSizeBytes();
    if (contentLength > limit) {
      throw new RequestTooLargeException(
          String.format(
              "Declared Content-Length: %d bytes, max allowed: %d bytes", contentLength, limit));
    }
    final MessageSerializer serializer = serializerRegistry.get(mapping.getFormat());
    if (serializer == null) {
      throw new IllegalStateException(
          "No serializer registered for format: " + mapping.getFormat());
    }
    try (final InputStream in = new LimitedInputStream(req.getInputStream(), limit)) {
      final Object body = serializer.deserializeFromStream(in, parameter.getType());
      if (body != null && mapping.isValidate()) {
        validationHandler.validate(body);
      }
      return body;
    }
  }

  @Override
  public void validate(Method method) throws RouteValidationException {
    final long bodyParams = Arrays.stream(method.getParameters()).filter(this::supports).count();
    if (bodyParams > 1) {
      throw new RouteValidationException(
          method, "Multiple @Body parameters detected, only one request body is allowed per route");
    }
  }
}
