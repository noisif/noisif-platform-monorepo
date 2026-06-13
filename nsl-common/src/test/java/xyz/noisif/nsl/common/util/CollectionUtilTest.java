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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class CollectionUtilTest {
  @Test
  @DisplayName("should merge and consume two sorted lists in correct order")
  void shouldMergeSortedLists() {
    // given
    final List<Integer> list1 = List.of(1, 3, 5);
    final List<Integer> list2 = List.of(2, 4, 6);
    final List<Integer> result = new ArrayList<>();
    final Comparator<Integer> comparator = Integer::compareTo;
    // when
    CollectionUtil.consumeMergedSorted(
        list1,
        list2,
        comparator,
        item -> {
          result.add(item);
          return true;
        });
    // then
    assertThat(result).containsExactly(1, 2, 3, 4, 5, 6);
  }

  @Test
  @DisplayName("should handle lists of different sizes")
  void shouldHandleDifferentSizes() {
    // given
    final List<Integer> list1 = List.of(1, 10);
    final List<Integer> list2 = List.of(2, 3, 4, 5);
    final List<Integer> result = new ArrayList<>();
    // when
    CollectionUtil.consumeMergedSorted(
        list1,
        list2,
        Integer::compareTo,
        item -> {
          result.add(item);
          return true;
        });
    // then
    assertThat(result).containsExactly(1, 2, 3, 4, 5, 10);
  }

  @Test
  @DisplayName("should stop consuming when predicate returns false")
  void shouldAbortWhenPredicateReturnsFalse() {
    // given
    final List<Integer> list1 = List.of(1, 10, 20);
    final List<Integer> list2 = List.of(5, 15, 25);
    final List<Integer> result = new ArrayList<>();
    // when
    CollectionUtil.consumeMergedSorted(
        list1,
        list2,
        Integer::compareTo,
        item -> {
          if (item >= 15) {
            return false;
          }
          result.add(item);
          return true;
        });
    // then
    assertThat(result).containsExactly(1, 5, 10);
    assertThat(result).doesNotContain(15, 20, 25);
  }

  @Test
  @DisplayName("should handle one empty list")
  void shouldHandleOneEmptyList() {
    // given
    final List<Integer> list1 = List.of(1, 2, 3);
    final List<Integer> list2 = List.of();
    final List<Integer> result = new ArrayList<>();
    // when
    CollectionUtil.consumeMergedSorted(
        list1,
        list2,
        Integer::compareTo,
        item -> {
          result.add(item);
          return true;
        });
    // then
    assertThat(result).containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("should do nothing when both lists are empty")
  void shouldDoNothingForBothEmpty() {
    // given
    final List<Integer> list1 = List.of();
    final List<Integer> list2 = List.of();
    final List<Integer> result = new ArrayList<>();
    // when
    CollectionUtil.consumeMergedSorted(
        list1,
        list2,
        Integer::compareTo,
        item -> {
          result.add(item);
          return true;
        });
    // then
    assertThat(result).isEmpty();
  }
}
