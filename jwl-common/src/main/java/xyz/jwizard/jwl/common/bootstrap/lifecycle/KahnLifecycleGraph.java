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
package xyz.jwizard.jwl.common.bootstrap.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.bootstrap.CriticalBootstrapException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

// Kahn’s algorithm, topological sorting with directed acyclic graph
// https://www.techiedelight.com/kahn-topological-sort-algorithm/
public class KahnLifecycleGraph implements LifecycleGraph {
  private static final Logger LOG = LoggerFactory.getLogger(KahnLifecycleGraph.class);

  private final Map<Class<?>, LifecycleHook> nodes = new LinkedHashMap<>();

  @Override
  public void addNode(LifecycleHook hook) {
    final Class<?> clazz = hook.getClass();
    if (nodes.containsKey(clazz)) {
      throw new IllegalArgumentException(
          "Hook of type [" + clazz.getSimpleName() + "] is already registered in the graph");
    }
    nodes.put(clazz, hook);
    LOG.debug("Registered lifecycle hook: [{}]", clazz.getSimpleName());
  }

  @Override
  public void addNodes(Collection<? extends LifecycleHook> hooks) {
    for (LifecycleHook hook : hooks) {
      addNode(hook);
    }
  }

  @Override
  public List<LifecycleHook> resolve() {
    LOG.info("Resolving lifecycle dependency graph for {} registered hooks", nodes.size());
    final Map<Class<?>, List<Class<?>>> adjacencyList = new LinkedHashMap<>();
    final Map<Class<?>, Integer> inDegrees = new LinkedHashMap<>();

    for (final Class<?> clazz : nodes.keySet()) {
      adjacencyList.put(clazz, new ArrayList<>());
      inDegrees.put(clazz, 0);
    }
    buildEdges(adjacencyList, inDegrees);
    final List<LifecycleHook> resolvedOrder = executeKahnAlgorithm(adjacencyList, inDegrees);
    LOG.info("Successfully resolved lifecycle graph");

    if (LOG.isDebugEnabled()) {
      final String orderString =
          resolvedOrder.stream()
              .map(hook -> hook.getClass().getSimpleName())
              .collect(Collectors.joining(" -> "));
      LOG.debug("Final execution order: {}", orderString);
    }
    return resolvedOrder;
  }

  private void buildEdges(
      Map<Class<?>, List<Class<?>>> adjacencyList, Map<Class<?>, Integer> inDegrees) {
    for (final LifecycleHook dependentHook : nodes.values()) {
      final Class<?> dependentClass = dependentHook.getClass();
      final List<Class<? extends LifecycleHook>> dependencies = dependentHook.dependsOn();
      if (!dependencies.isEmpty() && LOG.isDebugEnabled()) {
        final String depNames =
            dependencies.stream().map(Class::getSimpleName).collect(Collectors.joining(", "));
        LOG.debug("Hook [{}] depends on: [{}]", dependentClass.getSimpleName(), depNames);
      }
      for (final Class<? extends LifecycleHook> dependencyClass : dependencies) {
        if (!nodes.containsKey(dependencyClass)) {
          throw new CriticalBootstrapException(
              "Missing dependency, hook ["
                  + dependentClass.getSimpleName()
                  + "] requires ["
                  + dependencyClass.getSimpleName()
                  + "] but it is not registered in the graph");
        }
        adjacencyList.get(dependencyClass).add(dependentClass);
        inDegrees.put(dependentClass, inDegrees.get(dependentClass) + 1);
      }
    }
  }

  private List<LifecycleHook> executeKahnAlgorithm(
      Map<Class<?>, List<Class<?>>> adjacencyList, Map<Class<?>, Integer> inDegrees) {
    final Queue<Class<?>> readyQueue = new ArrayDeque<>();
    final List<LifecycleHook> sortedResult = new ArrayList<>();
    for (final Map.Entry<Class<?>, Integer> entry : inDegrees.entrySet()) {
      if (entry.getValue() == 0) {
        LOG.debug(
            "Hook [{}] has 0 blockers, added to initial ready queue",
            entry.getKey().getSimpleName());
        readyQueue.add(entry.getKey());
      }
    }
    while (!readyQueue.isEmpty()) {
      final Class<?> current = readyQueue.poll();
      sortedResult.add(nodes.get(current));
      LOG.debug("Resolved and appended: [{}]", current.getSimpleName());
      for (final Class<?> dependent : adjacencyList.get(current)) {
        final int newInDegree = inDegrees.get(dependent) - 1;
        inDegrees.put(dependent, newInDegree);
        if (newInDegree == 0) {
          LOG.debug("Hook [{}] is now unblocked, adding to ready queue", dependent.getSimpleName());
          readyQueue.add(dependent);
        }
      }
    }
    if (sortedResult.size() != nodes.size()) {
      throw new CriticalBootstrapException(
          "Graph resolution failed, only "
              + sortedResult.size()
              + " out of "
              + nodes.size()
              + " hooks were resolved; this indicates a circular dependency");
    }
    return sortedResult;
  }
}
