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
package xyz.noisif.nsl.common.util;

import xyz.noisif.nsl.common.bootstrap.CriticalBootstrapException;
import xyz.noisif.nsl.common.bootstrap.ForbiddenInstantiationException;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class Assert {
  private Assert() {
    throw new ForbiddenInstantiationException(Assert.class);
  }

  public static <T> void notNull(T object, String name) {
    state(object != null, "object '" + name + "' cannot be null");
  }

  public static <T> T notNullAndGet(T object, String name) {
    notNull(object, name);
    return object;
  }

  public static void notEmpty(Collection<?> collection, String name) {
    state(collection != null && !collection.isEmpty(), "collection '" + name + "' cannot be empty");
  }

  public static void notNullAll(Collection<?> collection, String name) {
    state(
        collection != null && collection.stream().allMatch(Objects::nonNull),
        "all elements in collection '" + name + "' cannot have nullable values");
  }

  public static void notNullAll(Map<?, ?> collection, String name) {
    notNullAll(collection.values(), name);
  }

  public static <T extends Number> void minMaxRange(T value, T minInc, T maxExc, String name) {
    state(
        value.doubleValue() >= minInc.doubleValue() && value.doubleValue() < maxExc.doubleValue(),
        "value '"
            + name
            + "' must be between "
            + minInc
            + " (inclusive) and "
            + maxExc
            + " (exclusive)");
  }

  public static <T extends Number> void greaterThan(T value, T greaterThan, String name) {
    state(
        value.doubleValue() > greaterThan.doubleValue(),
        "value '" + name + "' must be greater than " + greaterThan);
  }

  public static <T extends Number> void greaterOrEqualThan(
      T value, T greaterOrEqualThan, String name) {
    state(
        value.doubleValue() >= greaterOrEqualThan.doubleValue(),
        "value '" + name + "' must be greater or equal than " + greaterOrEqualThan);
  }

  public static <T extends Number> void lowerOrEqualThan(T value, T lowerOrEqualThan, String name) {
    state(
        value.doubleValue() <= lowerOrEqualThan.doubleValue(),
        "value '" + name + "' must be lower or equal than " + lowerOrEqualThan);
  }

  public static void state(boolean expression, String message) {
    if (!expression) {
      throw new CriticalBootstrapException(message);
    }
  }
}
