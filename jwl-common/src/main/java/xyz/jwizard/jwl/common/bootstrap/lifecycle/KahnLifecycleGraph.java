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
package xyz.jwizard.jwl.common.bootstrap.lifecycle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.bootstrap.CriticalBootstrapException;

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
                "Hook of type [" + clazz.getSimpleName() + "] is already registered in the graph"
            );
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
            final String orderString = resolvedOrder.stream()
                .map(hook -> hook.getClass().getSimpleName())
                .collect(Collectors.joining(" -> "));
            LOG.debug("Final execution order: {}", orderString);
        }
        return resolvedOrder;
    }

    private void buildEdges(Map<Class<?>, List<Class<?>>> adjacencyList,
                            Map<Class<?>, Integer> inDegrees) {
        for (final LifecycleHook dependentHook : nodes.values()) {
            final Class<?> dependentClass = dependentHook.getClass();
            final List<Class<? extends LifecycleHook>> dependencies = dependentHook.dependsOn();
            if (!dependencies.isEmpty() && LOG.isDebugEnabled()) {
                final String depNames = dependencies.stream()
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "));
                LOG.debug("Hook [{}] depends on: [{}]", dependentClass.getSimpleName(), depNames);
            }
            for (final Class<? extends LifecycleHook> dependencyClass : dependencies) {
                if (!nodes.containsKey(dependencyClass)) {
                    throw new CriticalBootstrapException(
                        "Missing dependency, hook [" + dependentClass.getSimpleName() +
                            "] requires [" + dependencyClass.getSimpleName() +
                            "] but it is not registered in the graph"
                    );
                }
                adjacencyList.get(dependencyClass).add(dependentClass);
                inDegrees.put(dependentClass, inDegrees.get(dependentClass) + 1);
            }
        }
    }

    private List<LifecycleHook> executeKahnAlgorithm(Map<Class<?>, List<Class<?>>> adjacencyList,
                                                     Map<Class<?>, Integer> inDegrees) {
        final Queue<Class<?>> readyQueue = new ArrayDeque<>();
        final List<LifecycleHook> sortedResult = new ArrayList<>();
        for (final Map.Entry<Class<?>, Integer> entry : inDegrees.entrySet()) {
            if (entry.getValue() == 0) {
                LOG.debug("Hook [{}] has 0 blockers, added to initial ready queue",
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
                    LOG.debug("Hook [{}] is now unblocked, adding to ready queue",
                        dependent.getSimpleName());
                    readyQueue.add(dependent);
                }
            }
        }
        if (sortedResult.size() != nodes.size()) {
            throw new CriticalBootstrapException(
                "Graph resolution failed, only " + sortedResult.size() + " out of " +
                    nodes.size() + " hooks were resolved; this indicates a circular dependency"
            );
        }
        return sortedResult;
    }
}
