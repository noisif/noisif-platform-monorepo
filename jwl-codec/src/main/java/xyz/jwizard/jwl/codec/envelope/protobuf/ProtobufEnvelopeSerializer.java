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
package xyz.jwizard.jwl.codec.envelope.protobuf;

import java.util.function.Function;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.EncodedPayloadVisitor;
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
    public SerializerFormat getBaseFormat() {
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
    public void serializeAndAcceptEnvelope(OpCode opCode, Object payload,
                                           EncodedPayloadVisitor visitor) {
        visitor.accept(serializeForSession(opCode, payload));
    }

    @Override
    public void acceptRaw(byte[] rawPayload, EncodedPayloadVisitor visitor) {
        visitor.accept(rawPayload);
    }

    @Override
    public MessageEnvelope<?> unwrap(byte[] payload, Function<Integer, Class<?>> typeResolver) {
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
