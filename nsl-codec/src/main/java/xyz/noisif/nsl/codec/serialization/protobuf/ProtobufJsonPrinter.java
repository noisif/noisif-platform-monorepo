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
package xyz.noisif.nsl.codec.serialization.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import xyz.noisif.nsl.codec.serialization.json.spi.serializer.RawJson;

public class ProtobufJsonPrinter {
  private final JsonFormat.Printer printer;

  public ProtobufJsonPrinter() {
    this.printer =
        JsonFormat.printer()
            .omittingInsignificantWhitespace() // minifies JSON by removing spaces and newlines
            .alwaysPrintFieldsWithNoPresence(); // includes default and empty values in the output
  }

  public RawJson toJson(MessageOrBuilder messageOrBuilder) {
    try {
      return RawJson.of(printer.print(messageOrBuilder));
    } catch (InvalidProtocolBufferException ex) {
      throw new ProtobufSerializerException("Failed to print Protobuf message to JSON", ex);
    }
  }
}
