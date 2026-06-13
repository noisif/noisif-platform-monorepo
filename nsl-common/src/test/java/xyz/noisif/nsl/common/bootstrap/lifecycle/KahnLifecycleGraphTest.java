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
package xyz.noisif.nsl.common.bootstrap.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.noisif.nsl.common.bootstrap.CriticalBootstrapException;
import xyz.noisif.nsl.common.di.ComponentProvider;
import xyz.noisif.nsl.common.reflect.ClassScanner;

import java.util.List;

class KahnLifecycleGraphTest {
  private KahnLifecycleGraph graph;

  @BeforeEach
  void setUp() {
    graph = new KahnLifecycleGraph();
  }

  @Test
  @DisplayName("should resolve independent hooks in insertion order")
  void shouldResolveIndependentHooksInInsertionOrder() {
    // given
    graph.addNode(new IndependentHook1());
    graph.addNode(new IndependentHook2());
    graph.addNode(new IndependentHook3());
    // when
    final List<LifecycleHook> result = graph.resolve();
    // then
    assertEquals(3, result.size());
    assertInstanceOf(IndependentHook1.class, result.get(0));
    assertInstanceOf(IndependentHook2.class, result.get(1));
    assertInstanceOf(IndependentHook3.class, result.get(2));
  }

  @Test
  @DisplayName("should resolve linear dependencies regardless of insertion order")
  void shouldResolveLinearDependenciesRegardlessOfInsertionOrder() {
    // given
    graph.addNode(new HookC());
    graph.addNode(new HookA());
    graph.addNode(new HookB());
    // when
    final List<LifecycleHook> result = graph.resolve();
    // then
    assertEquals(3, result.size());
    assertInstanceOf(HookA.class, result.get(0));
    assertInstanceOf(HookB.class, result.get(1));
    assertInstanceOf(HookC.class, result.get(2));
  }

  @Test
  @DisplayName("should resolve complex branching dependencies (DAG)")
  void shouldResolveComplexBranchingDependencies() {
    // given
    graph.addNode(new Branch2());
    graph.addNode(new Leaf());
    graph.addNode(new Root());
    graph.addNode(new Branch1());
    // when
    List<LifecycleHook> result = graph.resolve();
    // then
    assertEquals(4, result.size());
    assertInstanceOf(Root.class, result.get(0));
    assertInstanceOf(Leaf.class, result.get(3));
  }

  @Test
  @DisplayName("should throw exception when a required dependency is missing")
  void shouldThrowExceptionWhenDependencyIsMissing() {
    // given
    graph.addNode(new HookB());
    // when & then
    final CriticalBootstrapException exception =
        assertThrows(CriticalBootstrapException.class, () -> graph.resolve());
    assertTrue(exception.getMessage().contains("Missing dependency"));
    assertTrue(exception.getMessage().contains("requires [HookA]"));
  }

  @Test
  @DisplayName("should throw exception on circular dependency")
  void shouldThrowExceptionOnCircularDependency() {
    // given
    graph.addNode(new CycleA());
    graph.addNode(new CycleB());
    graph.addNode(new CycleC());
    // when & then
    final CriticalBootstrapException exception =
        assertThrows(CriticalBootstrapException.class, () -> graph.resolve());
    assertTrue(exception.getMessage().contains("indicates a circular dependency"));
  }

  @Test
  @DisplayName("should throw exception on duplicate hook registration")
  void shouldThrowExceptionOnDuplicateRegistration() {
    // given
    final HookA hook = new HookA();
    graph.addNode(hook);
    // when & then
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> graph.addNode(new HookA()));
    assertTrue(exception.getMessage().contains("is already registered"));
  }
}

class HookA implements LifecycleHook {
  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}

class HookB implements LifecycleHook {
  @Override
  public List<Class<? extends LifecycleHook>> dependsOn() {
    return List.of(HookA.class);
  }

  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}

class HookC implements LifecycleHook {
  @Override
  public List<Class<? extends LifecycleHook>> dependsOn() {
    return List.of(HookB.class);
  }

  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}

// DAG
class Root implements LifecycleHook {
  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}

class Branch1 implements LifecycleHook {
  @Override
  public List<Class<? extends LifecycleHook>> dependsOn() {
    return List.of(Root.class);
  }

  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}

class Branch2 implements LifecycleHook {
  @Override
  public List<Class<? extends LifecycleHook>> dependsOn() {
    return List.of(Root.class);
  }

  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}

class Leaf implements LifecycleHook {
  @Override
  public List<Class<? extends LifecycleHook>> dependsOn() {
    return List.of(Branch1.class, Branch2.class);
  }

  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}

// cycles (errors)
class CycleA implements LifecycleHook {
  @Override
  public List<Class<? extends LifecycleHook>> dependsOn() {
    return List.of(CycleC.class);
  }

  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}

class CycleB implements LifecycleHook {
  @Override
  public List<Class<? extends LifecycleHook>> dependsOn() {
    return List.of(CycleA.class);
  }

  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}

class CycleC implements LifecycleHook {
  @Override
  public List<Class<? extends LifecycleHook>> dependsOn() {
    return List.of(CycleB.class);
  }

  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}

// independent (no dependency)
class IndependentHook1 implements LifecycleHook {
  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}

class IndependentHook2 implements LifecycleHook {
  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}

class IndependentHook3 implements LifecycleHook {
  @Override
  public void onStart(ComponentProvider provider, ClassScanner scanner) {}
}
