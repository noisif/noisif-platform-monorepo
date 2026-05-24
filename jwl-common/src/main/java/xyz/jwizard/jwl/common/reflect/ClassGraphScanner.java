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
package xyz.jwizard.jwl.common.reflect;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.common.util.io.IoUtil;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

public class ClassGraphScanner implements ClassScanner {
    private static final Logger LOG = LoggerFactory.getLogger(ClassGraphScanner.class);
    private final ScanResult scanResult;

    public ClassGraphScanner(String... packages) {
        LOG.info("Initializing class scanner for package(s): {}", Arrays.asList(packages));
        scanResult = new ClassGraph()
            .enableAnnotationInfo()
            .acceptPackages(packages)
            // enable all info about classes (fields, methods, etc.)
            .enableAllInfo()
            .scan();
    }

    @Override
    public Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation) {
        return new HashSet<>(scanResult
            .getClassesWithAnnotation(annotation.getName())
            .loadClasses());
    }

    @Override
    public <T> Set<Class<? extends T>> getSubtypesOf(Class<T> type) {
        return new HashSet<>(getRawSubtypes(type).loadClasses(type));
    }

    @Override
    public <T> Set<Class<? extends T>> getInstantiableSubtypesOf(Class<T> type) {
        final ClassInfoList concreteClasses = getRawSubtypes(type)
            .filter(info -> !info.isAbstract() && !info.isInterface());
        return new HashSet<>(concreteClasses.loadClasses(type));
    }

    @Override
    public void close() {
        IoUtil.closeQuietly(scanResult);
    }

    private ClassInfoList getRawSubtypes(Class<?> type) {
        if (type.isInterface()) {
            return scanResult.getClassesImplementing(type.getName());
        } else {
            return scanResult.getSubclasses(type.getName());
        }
    }
}
