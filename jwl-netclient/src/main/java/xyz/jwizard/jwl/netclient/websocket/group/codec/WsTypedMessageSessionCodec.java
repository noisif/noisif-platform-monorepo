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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.UnsupportedDataTypeException;
import xyz.jwizard.jwl.codec.serialization.MessageSerializerException;
import xyz.jwizard.jwl.codec.serialization.SerializerFormat;
import xyz.jwizard.jwl.codec.serialization.TypedMessageSerializer;
import xyz.jwizard.jwl.common.util.CastUtil;

public class WsTypedMessageSessionCodec implements WsSessionCodec {
    private static final Logger LOG = LoggerFactory.getLogger(WsTypedMessageSessionCodec.class);

    private final TypedMessageSerializer<?> serializer;

    public WsTypedMessageSessionCodec(TypedMessageSerializer<?> serializer) {
        this.serializer = serializer;
    }

    @Override
    public WsSessionCodecMode getCurrentMode() {
        return WsSessionCodecMode.TYPED_MESSAGE;
    }

    @Override
    public void sendObject(Object payload, EncodedPayloadVisitor visitor) {
        try {
            serializer.serializeAndAccept(payload, visitor);
        } catch (UnsupportedDataTypeException | MessageSerializerException ex) {
            LOG.error("Message error for RAW {}: {}", serializer.getFormat().getFormatName(),
                ex.getMessage());
        } catch (Exception ex) {
            LOG.error("Unexpected error during processing RAW payload of type: {}",
                payload != null ? payload.getClass().getSimpleName() : "null", ex);
        }
    }

    @Override
    public <T> T parse(byte[] payload, Class<T> type) {
        return serializer.deserializePayload(CastUtil.unsafeCast(payload), type);
    }

    @Override
    public <T> T parse(String payload, Class<T> type) {
        return serializer.deserializePayload(CastUtil.unsafeCast(payload), type);
    }

    @Override
    public SerializerFormat getBaseFormat() {
        return serializer.getFormat();
    }

    @Override
    public DataType getCodecDataType() {
        return serializer.getCodecDataType();
    }
}
