/*
 * Copyright (c) 2022-2026 NOISIF. All Rights Reserved.
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
package xyz.noisif.nsl.netclient.websocket.group.codec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.codec.DataType;
import xyz.noisif.nsl.codec.EncodedPayloadVisitor;
import xyz.noisif.nsl.codec.UnsupportedDataTypeException;
import xyz.noisif.nsl.codec.serialization.MessageSerializerException;
import xyz.noisif.nsl.codec.serialization.SerializerFormat;
import xyz.noisif.nsl.codec.serialization.TypedMessageSerializer;
import xyz.noisif.nsl.common.util.CastUtil;

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
      LOG.error(
          "Message error for RAW {}: {}", serializer.getFormat().getFormatName(), ex.getMessage());
    } catch (Exception ex) {
      LOG.error(
          "Unexpected error during processing RAW payload of type: {}",
          payload != null ? payload.getClass().getSimpleName() : "null",
          ex);
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
