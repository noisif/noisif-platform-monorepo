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
package xyz.noisif.nsl.http.resolver;

import xyz.noisif.nsl.http.HttpRequest;
import xyz.noisif.nsl.http.annotation.RequestParam;
import xyz.noisif.nsl.http.route.MatchResult;

import java.lang.reflect.Parameter;

public class RequestParamResolver implements ArgumentResolver {
  @Override
  public boolean supports(Parameter parameter) {
    return parameter.isAnnotationPresent(RequestParam.class);
  }

  @Override
  public Object resolve(Parameter parameter, HttpRequest req, MatchResult match) {
    final RequestParam annotation = parameter.getAnnotation(RequestParam.class);
    final String paramName = annotation.value();

    String paramValue = req.getQueryParam(paramName);
    if (paramValue == null) {
      if (annotation.defaultValue().isEmpty()) {
        if (annotation.required()) {
          throw new IllegalArgumentException(
              "Required request parameter '" + paramName + "' is missing");
        }
        return null;
      }
      paramValue = annotation.defaultValue();
    }
    return ParameterConverter.parse(parameter.getType(), paramValue);
  }
}
