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
package xyz.noisif.nss.gateway.queue.listener;

import xyz.noisif.nsl.codec.serialization.SerializerFormat;
import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;
import xyz.noisif.nsl.codec.serialization.protobuf.ProtobufJsonPrinter;
import xyz.noisif.nsl.codec.serialization.protobuf.ProtobufSerializerException;
import xyz.noisif.nsl.contracts.protobuf.TranscodeProgressEvent;
import xyz.noisif.nsl.queue.QueueListener;
import xyz.noisif.nsl.queue.QueueTopology;
import xyz.noisif.nsl.queue.exchange.DefaultExchangeType;
import xyz.noisif.nsl.queue.retryable.FatalMessageProcessingException;
import xyz.noisif.nsl.websocket.broadcast.WsBroadcaster;
import xyz.noisif.nss.gateway.ws.GatewayOpCode;
import xyz.noisif.nss.gateway.ws.job.JobWsTopic;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
class TranscodeProgressEventsListener implements QueueListener<TranscodeProgressEvent> {
  private final WsBroadcaster wsBroadcaster;
  private final ProtobufJsonPrinter protobufJsonPrinter;

  @Inject
  TranscodeProgressEventsListener(
      WsBroadcaster wsBroadcaster, ProtobufJsonPrinter protobufJsonPrinter) {
    this.wsBroadcaster = wsBroadcaster;
    this.protobufJsonPrinter = protobufJsonPrinter;
  }

  @Override
  public String getQueueName() {
    return "transcoder.audio.progress.events";
  }

  @Override
  public Class<TranscodeProgressEvent> getMessageType() {
    return TranscodeProgressEvent.class;
  }

  @Override
  public void onMessage(TranscodeProgressEvent message) {
    try {
      wsBroadcaster.broadcast(
          new JobWsTopic(message.getJobId()),
          GatewayOpCode.AUDIO_TRANSCODE_PROGRESS,
          protobufJsonPrinter.toJson(message));
    } catch (ProtobufSerializerException ex) {
      throw new FatalMessageProcessingException(ex);
    }
  }

  @Override
  public SerializerFormat getFormat() {
    return StandardSerializerFormat.PROTOBUF;
  }

  @Override
  public QueueTopology getTopology() {
    return QueueTopology.builder()
        .durable(true)
        .exclusive(false)
        .autoDelete(false)
        .bindToExchange("transcoder.events", DefaultExchangeType.DIRECT, "audio.progress")
        .build();
  }
}
