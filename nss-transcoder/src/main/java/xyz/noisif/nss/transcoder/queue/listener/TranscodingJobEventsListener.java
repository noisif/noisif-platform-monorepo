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
package xyz.noisif.nss.transcoder.queue.listener;

import xyz.noisif.nsl.codec.serialization.SerializerFormat;
import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;
import xyz.noisif.nsl.contracts.protobuf.ProcessAudioCommand;
import xyz.noisif.nsl.contracts.protobuf.TranscodeProgressEvent;
import xyz.noisif.nsl.contracts.protobuf.TranscodeStatus;
import xyz.noisif.nsl.queue.MessagePublisher;
import xyz.noisif.nsl.queue.QueueListener;
import xyz.noisif.nsl.queue.QueueTopology;
import xyz.noisif.nsl.queue.exchange.DefaultExchangeType;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
class TranscodingJobEventsListener implements QueueListener<ProcessAudioCommand> {
  private final MessagePublisher messagePublisher;

  @Inject
  TranscodingJobEventsListener(MessagePublisher messagePublisher) {
    this.messagePublisher = messagePublisher;
  }

  @Override
  public String getQueueName() {
    return "transcoder.audio.process.jobs";
  }

  @Override
  public Class<ProcessAudioCommand> getMessageType() {
    return ProcessAudioCommand.class;
  }

  @Override
  public SerializerFormat getFormat() {
    return StandardSerializerFormat.PROTOBUF;
  }

  @Override
  public void onMessage(ProcessAudioCommand message) {
    final TranscodeProgressEvent.Builder builder =
        TranscodeProgressEvent.newBuilder()
            .setJobId(message.getJobId())
            .setUserId(message.getUserId())
            .setStatus(TranscodeStatus.PROCESSING);

    for (int i = 0; i < 100; i++) {
      builder.setProgress(i);
      messagePublisher.publish(
          "transcoder.events",
          "audio.progress",
          builder.build(),
          StandardSerializerFormat.PROTOBUF);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    builder.setStatus(TranscodeStatus.COMPLETED);
    builder.setProgress(100);
    messagePublisher.publish(
        "transcoder.events", "audio.progress", builder.build(), StandardSerializerFormat.PROTOBUF);
  }

  @Override
  public QueueTopology getTopology() {
    return QueueTopology.builder()
        .durable(true)
        .exclusive(false) // for many transcoding workers
        .autoDelete(false)
        .withDeadLetter()
        .bindToExchange("transcoder.commands", DefaultExchangeType.DIRECT, "audio.process")
        .prefetchCount(1) // single prefetch per job in queue
        .build();
  }
}
