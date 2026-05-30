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
package xyz.jwizard.jwl.http.resolver;

import xyz.jwizard.jwl.common.util.PathUtil;
import xyz.jwizard.jwl.http.HttpRequest;
import xyz.jwizard.jwl.http.annotation.HttpController;
import xyz.jwizard.jwl.http.annotation.PathVariable;
import xyz.jwizard.jwl.http.annotation.RequestMapping;
import xyz.jwizard.jwl.http.exception.RouteValidationException;
import xyz.jwizard.jwl.http.route.MatchResult;
import xyz.jwizard.jwl.http.route.Router;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;

public class PathVariableResolver implements ArgumentResolver {
  private final Router router;

  public PathVariableResolver(Router router) {
    this.router = router;
  }

  @Override
  public boolean supports(Parameter parameter) {
    return parameter.isAnnotationPresent(PathVariable.class);
  }

  @Override
  public Object resolve(Parameter parameter, HttpRequest req, MatchResult match) {
    final PathVariable annotation = parameter.getAnnotation(PathVariable.class);
    final String name = annotation.value();
    final String value = match.variables().get(name);
    if (value == null && annotation.required()) {
      // should never be throws, only if matcher ergine somehow has an error
      throw new IllegalArgumentException("Required path variable '" + name + "' is missing");
    }
    if (value == null) {
      return null;
    }
    return ParameterConverter.parse(parameter.getType(), value);
  }

  @Override
  public void validate(Method method) throws RouteValidationException {
    final RequestMapping mapping = method.getAnnotation(RequestMapping.class);
    final HttpController controller =
        method.getDeclaringClass().getAnnotation(HttpController.class);

    final String httpMethod = mapping.method().name();
    final String basePath = controller != null ? controller.value() : "";

    for (final String path : mapping.value()) {
      final String fullPath = PathUtil.combinePaths(basePath, path);
      final Set<String> registeredVars = router.getVariableNamesFor(httpMethod, fullPath);
      for (Parameter parameter : method.getParameters()) {
        if (!supports(parameter)) {
          continue;
        }
        final String nameInAnnotation = parameter.getAnnotation(PathVariable.class).value();
        if (!registeredVars.contains(nameInAnnotation)) {
          throw new RouteValidationException(
              method,
              String.format(
                  "PathVariable '%s' not found in registered route pattern: %s",
                  nameInAnnotation, fullPath));
        }
      }
    }
  }
}
