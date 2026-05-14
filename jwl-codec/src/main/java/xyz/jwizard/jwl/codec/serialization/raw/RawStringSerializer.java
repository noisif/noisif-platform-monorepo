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
package xyz.jwizard.jwl.codec.serialization.raw;

import java.nio.charset.StandardCharsets;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.serialization.MessageSerializer;
import xyz.jwizard.jwl.codec.serialization.MessageSerializerException;
import xyz.jwizard.jwl.codec.serialization.SerializerFormat;
import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;
import xyz.jwizard.jwl.codec.serialization.TypedMessageSerializer;
import xyz.jwizard.jwl.common.util.CastUtil;

public class RawStringSerializer implements MessageSerializer, TypedMessageSerializer<String> {
    private RawStringSerializer() {
    }

    public static RawStringSerializer createDefault() {
        return new RawStringSerializer();
    }

    @Override
    public byte[] serializeToBytes(Object value) {
        return serializePayload(value).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserializeFromBytes(byte[] bytes, Class<T> type) {
        final String str = new String(bytes, StandardCharsets.UTF_8);
        return deserializePayload(str, type);
    }

    @Override
    public SerializerFormat format() {
        return StandardSerializerFormat.RAW;
    }

    @Override
    public String serializePayload(Object payload) {
        if (payload == null) {
            return "";
        }
        if (payload instanceof String str) {
            return str;
        }
        throw new MessageSerializerException(
            "RawStringSerializer can only handle String, but received: " +
                payload.getClass().getName()
        );
    }

    @Override
    public <T> T deserializePayload(String payload, Class<T> type) {
        if (type.isAssignableFrom(String.class)) {
            return CastUtil.unsafeCast(payload);
        }
        throw new MessageSerializerException(
            "RawStringSerializer can only deserialize to String.class, but requested: " +
                type.getName()
        );
    }

    @Override
    public void serializeAndAccept(Object payload, EncodedPayloadVisitor visitor) {
        visitor.accept(serializePayload(payload));
    }

    @Override
    public DataType getCodecDataType() {
        return DataType.TEXT;
    }
}
