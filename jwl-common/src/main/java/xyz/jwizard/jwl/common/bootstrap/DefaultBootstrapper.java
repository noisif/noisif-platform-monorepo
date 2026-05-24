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
package xyz.jwizard.jwl.common.bootstrap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.bootstrap.lifecycle.KahnLifecycleGraph;
import xyz.jwizard.jwl.common.bootstrap.lifecycle.LifecycleGraph;
import xyz.jwizard.jwl.common.bootstrap.lifecycle.LifecycleHook;
import xyz.jwizard.jwl.common.di.ApplicationContext;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.di.GuiceComponentProvider;
import xyz.jwizard.jwl.common.reflect.ClassGraphScanner;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.common.util.ArrayUtil;
import xyz.jwizard.jwl.common.util.io.IoUtil;

public class DefaultBootstrapper {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultBootstrapper.class);

    private static final CountDownLatch SHUTDOWN_LATCH = new CountDownLatch(1);
    private static final LifecycleGraph LIFECYCLE_GRAPH = new KahnLifecycleGraph();
    private static final String JWL_SUFFIX = ".jwl";

    private DefaultBootstrapper() {
        throw new ForbiddenInstantiationException(DefaultBootstrapper.class);
    }

    public static void runNonBlocking(Class<?> primarySource) {
        run(primarySource, false);
    }

    public static void run(Class<?> primarySource) {
        run(primarySource, true);
    }

    public static void run(Class<?> primarySource, boolean wait) {
        LOG.info("Application runtime mode: {}", wait ? "blocking" : "non-blocking");

        final String[] packagesToScan = getPackagesToScan(primarySource);
        LOG.info("Start bootstrapping application on package(s): {}",
            Arrays.asList(packagesToScan));

        final long startTime = System.currentTimeMillis();
        try (final ClassScanner scanner = new ClassGraphScanner(packagesToScan)) {
            final ApplicationContext context = ApplicationContext.create(scanner, Map.of(
                ComponentProvider.class, GuiceComponentProvider.class
            ), Map.of(
                ClassScanner.class, scanner
            ));
            final List<? extends LifecycleHook> hooks = discoverAndSortHooks(scanner, context);
            registerShutdownHook(hooks, wait);
            startHooks(hooks, context);
            awaitTermination(startTime, wait);
        } catch (CriticalBootstrapException ex) {
            LOG.error("FATAL ERROR DURING APPLICATION STARTUP: {}", ex.getMessage(), ex);
            System.exit(1);
        } catch (InterruptedException ex) {
            LOG.warn("Main thread interrupted, initiating shutdown");
            Thread.currentThread().interrupt(); // restore interrupt flag
        } catch (Exception ex) {
            LOG.error("Error during startup: ", ex);
        }
    }

    private static List<? extends LifecycleHook> discoverAndSortHooks(ClassScanner scanner,
                                                                      ApplicationContext context) {
        final List<LifecycleHook> rawHooks = scanner.getSubtypesOf(LifecycleHook.class).stream()
            .map(clazz -> (LifecycleHook) context.getComponentProvider().getInstance(clazz))
            .toList();
        LIFECYCLE_GRAPH.addNodes(rawHooks);
        return LIFECYCLE_GRAPH.resolve();
    }

    private static void registerShutdownHook(List<? extends LifecycleHook> hooks, boolean wait) {
        final GracefulShutdownHook shutdownThread = new GracefulShutdownHook(hooks,
            SHUTDOWN_LATCH, wait);
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    private static void startHooks(List<? extends LifecycleHook> hooks,
                                   ApplicationContext context) {
        for (final LifecycleHook hook : hooks) {
            final String name = hook.getClass().getSimpleName();
            try {
                LOG.debug("Starting lifecycle hook: {}", name);
                hook.onStart(context.getComponentProvider(), context.getScanner());
            } catch (Exception ex) {
                throw new CriticalBootstrapException("Failed to start hook: " + name, ex);
            }
        }
    }

    private static void awaitTermination(long startTime, boolean wait) throws InterruptedException {
        final long durationMs = System.currentTimeMillis() - startTime;
        LOG.info("Bootstrapped and started successfully in {}s",
            String.format("%.3f", durationMs / 1000.0));
        IoUtil.thrownQuietly(System.in::close);
        if (wait) {
            SHUTDOWN_LATCH.await();
        }
    }

    private static String[] getPackagesToScan(Class<?> primarySource) {
        final Set<String> packagesToScan = new HashSet<>();
        packagesToScan.add(primarySource.getPackageName());
        String jwlRoot = DefaultBootstrapper.class.getPackageName();
        if (jwlRoot.contains(JWL_SUFFIX)) {
            jwlRoot = jwlRoot.substring(0, jwlRoot.indexOf(JWL_SUFFIX) + JWL_SUFFIX.length());
        }
        packagesToScan.add(jwlRoot);
        if (primarySource.isAnnotationPresent(AppBootstrapper.class)) {
            final AppBootstrapper appInitializer = primarySource
                .getAnnotation(AppBootstrapper.class);
            Collections.addAll(packagesToScan, appInitializer.scanPackages());
        }
        return ArrayUtil.toArray(packagesToScan, String.class);
    }
}
