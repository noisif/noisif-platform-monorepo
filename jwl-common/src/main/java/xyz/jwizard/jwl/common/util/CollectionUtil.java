/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jwizard.jwl.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import xyz.jwizard.jwl.common.bootstrap.ForbiddenInstantiationException;

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

    // two-pointers algorithm (zero allocation), for O(n), instead of streams and O(nlogn)
    public static <T> void consumeMergedSorted(List<T> list1, List<T> list2,
                                               Comparator<? super T> comparator,
                                               Predicate<T> action) {
        if (action == null) {
            return;
        }
        final List<T> l1 = (list1 == null) ? List.of() : list1;
        final List<T> l2 = (list2 == null) ? List.of() : list2;

        final Comparator<? super T> comp = (comparator != null)
            ? comparator
            : CastUtil.unsafeCast(Comparator.naturalOrder());

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
