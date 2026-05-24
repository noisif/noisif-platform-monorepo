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
package xyz.jwizard.jwl.netclient.websocket.group.codec;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import xyz.jwizard.jwl.codec.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.UnifiedMessageCodec;
import xyz.jwizard.jwl.codec.envelope.MessageEnvelope;
import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.codec.serialization.MessageSerializerException;

public interface WsSessionCodec extends UnifiedMessageCodec {
    WsSessionCodecMode getCurrentMode();

    default void sendObject(Object payload, EncodedPayloadVisitor visitor) {
        throw throwNotSupported();
    }

    default <T> T parse(byte[] payload, Class<T> type) {
        throw throwNotSupported();
    }

    default <T> T parse(String payload, Class<T> type) {
        throw throwNotSupported();
    }

    @Override
    default void serializeAndAcceptEnvelope(OpCode opCode, Object data,
                                            EncodedPayloadVisitor visitor) {
        throw throwNotSupported();
    }

    @Override
    default MessageEnvelope<?> unwrap(byte[] payload, Function<Integer, Class<?>> typeResolver) {
        throw throwNotSupported();
    }

    @Override
    default MessageEnvelope<?> unwrap(String payload, Function<Integer, Class<?>> typeResolver) {
        throw throwNotSupported();
    }

    default MessageSerializerException throwNotSupported() {
        final List<String> notSupported = Arrays.stream(WsSessionCodecMode.values())
            .filter(codec -> !codec.equals(getCurrentMode()))
            .map(Enum::name)
            .toList();
        return new MessageSerializerException("""
            This session operates in %s mode and does not support %s operation(s)
            """.formatted(getCurrentMode(), notSupported)
        );
    }
}
