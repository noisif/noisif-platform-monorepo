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
package xyz.jwizard.jwl.codec.envelope.protobuf;

import java.util.function.Function;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.envelope.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.MessageEnvelope;
import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.codec.serialization.SerializerFormat;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;
import xyz.jwizard.jwl.codec.serialization.protobuf.ProtobufSerializer;
import xyz.jwizard.jwl.codec.serialization.protobuf.ProtobufSerializerException;

public class ProtobufEnvelopeSerializer implements EnvelopeSerializer<byte[]> {
    private final ProtobufSerializer protobufSerializer;

    private ProtobufEnvelopeSerializer(ProtobufSerializer protobufSerializer) {
        this.protobufSerializer = protobufSerializer;
    }

    public static ProtobufEnvelopeSerializer createDefault(ProtobufSerializer protobufSerializer) {
        return new ProtobufEnvelopeSerializer(protobufSerializer);
    }

    @Override
    public SerializerFormat baseFormat() {
        return StandardSerializerFormat.PROTOBUF;
    }

    @Override
    public DataType getCodecDataType() {
        return DataType.BINARY;
    }

    @Override
    public byte[] serializeForSession(OpCode opCode, Object payload) {
        return serializeEnvelopeAsBytes(opCode, payload);
    }

    @Override
    public byte[] serializeEnvelopeAsBytes(OpCode opCode, Object payload) {
        final ByteString dataBytes;
        if (payload instanceof MessageLite msg) {
            dataBytes = msg.toByteString();
        } else if (payload == null) {
            dataBytes = ByteString.EMPTY;
        } else {
            throw new ProtobufSerializerException("Payload must be a Protobuf MessageLite");
        }
        final RawWsEnvelope envelope = RawWsEnvelope.newBuilder()
            .setOp(opCode.getCode())
            .setData(dataBytes)
            .build();
        return envelope.toByteArray();
    }

    @Override
    public void serializeAndAccept(OpCode opCode, Object payload, EncodedPayloadVisitor visitor) {
        visitor.accept(serializeForSession(opCode, payload));
    }

    @Override
    public void acceptRaw(byte[] rawPayload, EncodedPayloadVisitor visitor) {
        visitor.accept(rawPayload);
    }

    @Override
    public MessageEnvelope<?> deserializeEnvelope(byte[] payload,
                                                  Function<Integer, Class<?>> typeResolver) {
        final RawWsEnvelope protoEnvelope = protobufSerializer
            .deserializeFromBytes(payload, RawWsEnvelope.class);
        final int op = protoEnvelope.getOp();
        final Class<?> dataType = typeResolver.apply(op);
        if (dataType == null) {
            return new MessageEnvelope<>(op, null); // checked in ActionRouterWsMessageListener
        }
        Object data = null;
        if (!protoEnvelope.getData().isEmpty() && dataType != Void.class) {
            byte[] innerData = protoEnvelope.getData().toByteArray();
            data = protobufSerializer.deserializeFromBytes(innerData, dataType);
        }
        return new MessageEnvelope<>(op, data);
    }
}
