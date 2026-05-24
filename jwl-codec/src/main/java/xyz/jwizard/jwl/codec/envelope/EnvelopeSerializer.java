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
package xyz.jwizard.jwl.codec.envelope;

import java.util.function.Function;

import xyz.jwizard.jwl.codec.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.UnifiedMessageCodec;
import xyz.jwizard.jwl.codec.UnsupportedDataTypeException;

public interface EnvelopeSerializer<T> extends UnifiedMessageCodec {
    T serializeForSession(OpCode opCode, Object payload);

    byte[] serializeEnvelopeAsBytes(OpCode opCode, Object payload);

    default String serializeEnvelopeAsString(OpCode opCode, Object payload) {
        throw new UnsupportedDataTypeException("Text frames are not supported by " + getFormat());
    }

    void acceptRaw(byte[] rawPayload, EncodedPayloadVisitor visitor);

    @Override
    default MessageEnvelope<?> unwrap(String payload, Function<Integer, Class<?>> typeResolver) {
        throw new UnsupportedDataTypeException("Text frames are not supported by " + getFormat());
    }
}
