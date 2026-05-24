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
package xyz.jwizard.jwl.codec.serialization.protobuf;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.serialization.MessageSerializer;
import xyz.jwizard.jwl.codec.serialization.SerializerFormat;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;
import xyz.jwizard.jwl.codec.serialization.TypedMessageSerializer;
import xyz.jwizard.jwl.common.reflect.ClassScanner;
import xyz.jwizard.jwl.common.util.CastUtil;

public class ProtobufSerializer implements MessageSerializer, TypedMessageSerializer<byte[]> {
    private static final Logger LOG = LoggerFactory.getLogger(ProtobufSerializer.class);
    private static final Map<Class<?>, Parser<?>> PARSER_CACHE = new ConcurrentHashMap<>();

    private ProtobufSerializer(ClassScanner scanner) {
        registerAllFrom(scanner);
    }

    public static ProtobufSerializer createDefault(ClassScanner scanner) {
        return new ProtobufSerializer(scanner);
    }

    @Override
    public byte[] serializeToBytes(Object value) {
        if (value == null) {
            return new byte[0];
        }
        if (value instanceof MessageLite message) {
            return message.toByteArray();
        }
        throw new ProtobufSerializerException(
            "ProtobufSerializer can only handle com.google.protobuf.MessageLite, but received: "
                + value.getClass().getName()
        );
    }

    @Override
    public <T> T deserializeFromBytes(byte[] bytes, Class<T> type) {
        try {
            return CastUtil.unsafeCast(getParser(type).parseFrom(bytes));
        } catch (InvalidProtocolBufferException ex) {
            throw new ProtobufSerializerException("Stream read error", ex);
        }
    }

    @Override
    public <T> T deserializeFromStream(InputStream in, Class<T> type) {
        try {
            return CastUtil.unsafeCast(getParser(type).parseFrom(in));
        } catch (InvalidProtocolBufferException ex) {
            throw new ProtobufSerializerException("Stream read error", ex);
        }
    }

    @Override
    public SerializerFormat getFormat() {
        return StandardSerializerFormat.PROTOBUF;
    }

    private Parser<?> getParser(Class<?> type) {
        final Parser<?> parser = PARSER_CACHE.get(type);
        if (parser == null) {
            throw new ProtobufSerializerException("Type not registered: " + type.getName());
        }
        LOG.debug("Cache hit for protobuf parser: {}", type.getSimpleName());
        return parser;
    }

    private void registerAllFrom(ClassScanner scanner) {
        final Set<Class<? extends MessageLite>> protoClasses = scanner
            .getSubtypesOf(MessageLite.class);
        for (final Class<? extends MessageLite> type : protoClasses) {
            if (!isInstantiableMessage(type)) {
                continue;
            }
            try {
                final Method method = type.getMethod("parser");
                PARSER_CACHE.put(type, (Parser<?>) method.invoke(null));
                LOG.debug("Registered protobuf parser for: {}", type.getSimpleName());
            } catch (Exception ex) {
                throw new ProtobufSerializerException("Failed to register: " + type.getName(), ex);
            }
        }
        LOG.info("Registered {} protobuf message parser(s) in cache", PARSER_CACHE.size());
    }

    private boolean isInstantiableMessage(Class<?> type) {
        return !type.isInterface()
            && !Modifier.isAbstract(type.getModifiers())
            && !type.getName().startsWith("com.google.protobuf");
    }

    @Override
    public DataType getCodecDataType() {
        return DataType.BINARY;
    }

    @Override
    public byte[] serializePayload(Object payload) {
        return serializeToBytes(payload);
    }

    @Override
    public <T> T deserializePayload(byte[] payload, Class<T> type) {
        return deserializeFromBytes(payload, type);
    }

    @Override
    public void serializeAndAccept(Object payload, EncodedPayloadVisitor visitor) {
        visitor.accept(serializePayload(payload));
    }
}
