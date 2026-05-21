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

import java.util.function.Function;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.MessageEnvelope;
import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.codec.serialization.SerializerFormat;

public class WsEnvelopeSessionCodec implements WsSessionCodec {
    private final EnvelopeSerializer<?> serializer;

    public WsEnvelopeSessionCodec(EnvelopeSerializer<?> serializer) {
        this.serializer = serializer;
    }

    @Override
    public WsSessionCodecMode getCurrentMode() {
        return WsSessionCodecMode.ENVELOPE_MESSAGE;
    }

    @Override
    public void serializeAndAcceptEnvelope(OpCode opCode, Object data,
                                           EncodedPayloadVisitor visitor) {
        serializer.serializeAndAcceptEnvelope(opCode, data, visitor);
    }

    @Override
    public SerializerFormat getBaseFormat() {
        return serializer.getBaseFormat();
    }

    @Override
    public MessageEnvelope<?> unwrap(byte[] payload, Function<Integer, Class<?>> typeResolver) {
        return serializer.unwrap(payload, typeResolver);
    }

    @Override
    public MessageEnvelope<?> unwrap(String payload, Function<Integer, Class<?>> typeResolver) {
        return serializer.unwrap(payload, typeResolver);
    }

    @Override
    public DataType getCodecDataType() {
        return serializer.getCodecDataType();
    }
}
