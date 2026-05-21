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
