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
package xyz.jwizard.jwl.http.resolver.body;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.serialization.SerializerFormat;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;
import xyz.jwizard.jwl.common.util.CollectionUtil;
import xyz.jwizard.jwl.common.util.StringUtil;
import xyz.jwizard.jwl.common.util.math.MemSize;
import xyz.jwizard.jwl.common.util.math.MemUnit;

enum BodyMediaSerializer {
    JSON(
        StandardSerializerFormat.JSON,
        true,
        MemSize.of(2, MemUnit.MB),
        null
    ),
    PROTOBUF(
        StandardSerializerFormat.PROTOBUF,
        false,
        MemSize.of(1, MemUnit.MB),
        null,
        "application/x-protobuf"
    ),
    RAW(
        StandardSerializerFormat.RAW,
        false,
        MemSize.of(5, MemUnit.MB),
        byte[].class,
        "image/*"
    ),
    ;

    private static final Logger LOG = LoggerFactory.getLogger(BodyMediaSerializer.class);
    private static final BodyMediaSerializer DEFAULT_MAPPING = JSON;

    // for fast O(1) search
    private static final Map<Class<?>, BodyMediaSerializer> BY_CLASS = new HashMap<>();
    private static final Map<String, BodyMediaSerializer> BY_EXACT_TYPE = new HashMap<>();
    private static final Map<String, BodyMediaSerializer> BY_PREFIX_TYPE = new HashMap<>();

    static {
        for (final BodyMediaSerializer mapping : values()) {
            registerClassMapping(mapping);
            registerContentTypeMappings(mapping);
        }
        LOG.info("BodyMediaSerializer cache initialized ({} class, {} exact, {} wildcard mappings)",
            BY_CLASS.size(), BY_EXACT_TYPE.size(), BY_PREFIX_TYPE.size());
    }

    private final SerializerFormat format;
    private final boolean validate;
    private final long maxSizeBytes;
    private final Class<?> targetClass;
    private final List<String> contentTypes;

    BodyMediaSerializer(SerializerFormat format, boolean validate, long maxSizeBytes,
                        Class<?> targetClass, String... contentTypes) {
        this.format = format;
        this.validate = validate;
        this.maxSizeBytes = maxSizeBytes;
        this.targetClass = targetClass;
        this.contentTypes = CollectionUtil.listOf(format.getMimeType(), contentTypes);
    }

    static BodyMediaSerializer resolve(Class<?> targetType, String contentType) {
        final BodyMediaSerializer classMapping = BY_CLASS.get(targetType);
        if (classMapping != null) {
            return classMapping;
        }
        if (contentType == null) {
            return DEFAULT_MAPPING;
        }
        final BodyMediaSerializer exactMapping = BY_EXACT_TYPE.get(contentType);
        if (exactMapping != null) {
            return exactMapping;
        }
        final int slashIdx = contentType.indexOf('/');
        if (slashIdx != -1) {
            final String prefix = contentType.substring(0, slashIdx + 1);
            final BodyMediaSerializer prefixMapping = BY_PREFIX_TYPE.get(prefix);
            if (prefixMapping != null) {
                return prefixMapping;
            }
        }
        return DEFAULT_MAPPING;
    }

    private static void registerClassMapping(BodyMediaSerializer mapping) {
        if (mapping.targetClass != null) {
            BY_CLASS.put(mapping.targetClass, mapping);
            LOG.trace("Registered class mapping: {} -> {}",
                mapping.targetClass.getSimpleName(), mapping.name());
        }
    }

    private static void registerContentTypeMappings(BodyMediaSerializer mapping) {
        if (mapping.contentTypes == null || mapping.contentTypes.isEmpty()) {
            return;
        }
        for (final String type : mapping.contentTypes) {
            final String normalizedType = StringUtil.toLowerCase(type);
            if (normalizedType.endsWith("/*")) {
                final String prefix = normalizedType.substring(0, normalizedType.length() - 1);
                BY_PREFIX_TYPE.put(prefix, mapping);
                LOG.trace("Registered wildcard mapping: {}* -> {}", prefix, mapping.name());
            } else {
                BY_EXACT_TYPE.put(normalizedType, mapping);
                LOG.trace("Registered exact mapping: {} -> {}", normalizedType, mapping.name());
            }
        }
    }

    SerializerFormat getFormat() {
        return format;
    }

    boolean isValidate() {
        return validate;
    }

    long getMaxSizeBytes() {
        return maxSizeBytes;
    }
}
