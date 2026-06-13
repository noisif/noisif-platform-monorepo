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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

enum ParameterConverter {
  STRING(String.class, value -> value),
  INTEGER(Integer.class, Integer::valueOf),
  LONG(Long.class, Long::valueOf),
  BOOLEAN(
      Boolean.class,
      value -> {
        if ("true".equalsIgnoreCase(value)) {
          return true;
        }
        if ("false".equalsIgnoreCase(value)) {
          return false;
        }
        throw new IllegalArgumentException(
            "Invalid boolean value: '" + value + "', expected 'true' or 'false'");
      }),
  DOUBLE(Double.class, Double::valueOf),
  ;

  private static final Logger LOG = LoggerFactory.getLogger(ParameterConverter.class);

  // for fast O(1) search
  private static final Map<Class<?>, ParameterConverter> LOOKUP = new HashMap<>();

  static {
    for (final ParameterConverter converter : values()) {
      LOOKUP.put(converter.targetType, converter);
      LOG.trace(
          "Registered parameter converter: {} -> {}",
          converter.targetType.getSimpleName(),
          converter.name());
    }
    LOG.info("ParameterConverter cache initialized with {} mapping(s)", LOOKUP.size());
  }

  private final Class<?> targetType;
  private final Function<String, Object> converterFunction;

  ParameterConverter(Class<?> targetType, Function<String, Object> converterFunction) {
    this.targetType = targetType;
    this.converterFunction = converterFunction;
  }

  static Object parse(Class<?> targetType, String value) {
    if (value == null) {
      return null;
    }
    final ParameterConverter converter = LOOKUP.get(targetType);
    if (converter == null) {
      throw new IllegalArgumentException("Unsupported argument type: " + targetType.getName());
    }
    return converter.converterFunction.apply(value);
  }
}
