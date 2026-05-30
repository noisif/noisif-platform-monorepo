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
package xyz.jwizard.jwl.common.util;

import xyz.jwizard.jwl.common.bootstrap.ForbiddenInstantiationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class CollectionUtil {
  private CollectionUtil() {
    throw new ForbiddenInstantiationException(CollectionUtil.class);
  }

  @SafeVarargs
  public static <T> Set<T> linkedSetOf(T... elements) {
    return new LinkedHashSet<>(Arrays.asList(elements));
  }

  @SafeVarargs
  public static <T> List<T> listOf(T first, T... rest) {
    if (first == null && (rest == null || rest.length == 0)) {
      return List.of();
    }
    final List<T> result = new ArrayList<>();
    if (first != null) {
      result.add(first);
    }
    if (rest != null) {
      Collections.addAll(result, rest);
    }
    return Collections.unmodifiableList(result);
  }

  // two-pointers algorithm (zero allocation), for O(n), instead of streams and
  // O(nlogn)
  public static <T> void consumeMergedSorted(
      List<T> list1, List<T> list2, Comparator<? super T> comparator, Predicate<T> action) {
    if (action == null) {
      return;
    }
    final List<T> l1 = (list1 == null) ? List.of() : list1;
    final List<T> l2 = (list2 == null) ? List.of() : list2;

    final Comparator<? super T> comp =
        (comparator != null) ? comparator : CastUtil.unsafeCast(Comparator.naturalOrder());

    final int size1 = l1.size();
    final int size2 = l2.size();

    if (size1 == 0 && size2 == 0) {
      return;
    }
    int index1 = 0;
    int index2 = 0;
    while (index1 < size1 || index2 < size2) {
      T nextItem;
      if (index1 == size1) {
        nextItem = l2.get(index2++);
      } else if (index2 == size2) {
        nextItem = l1.get(index1++);
      } else if (comp.compare(l1.get(index1), l2.get(index2)) <= 0) {
        nextItem = l1.get(index1++);
      } else {
        nextItem = l2.get(index2++);
      }
      if (!action.test(nextItem)) {
        break;
      }
    }
  }

  public static String getFirstSafety(List<String> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    final String first = values.getFirst();
    return (first == null || first.isBlank()) ? null : first;
  }
}
