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
package xyz.noisif.nss.api.http.rest;

import xyz.noisif.nsl.codec.serialization.StandardSerializerFormat;
import xyz.noisif.nsl.contracts.protobuf.ProcessAudioCommand;
import xyz.noisif.nsl.http.ResponseEntity;
import xyz.noisif.nsl.http.annotation.HttpController;
import xyz.noisif.nsl.http.annotation.PathVariable;
import xyz.noisif.nsl.http.annotation.RequestMapping;
import xyz.noisif.nsl.net.http.HttpMethod;
import xyz.noisif.nsl.queue.MessagePublisher;

import jakarta.inject.Inject;

@HttpController
class AudioFileController {
  private final MessagePublisher messagePublisher;

  @Inject
  AudioFileController(MessagePublisher messagePublisher) {
    this.messagePublisher = messagePublisher;
  }

  @RequestMapping(value = "/api/audio/job/{jobId}", method = HttpMethod.POST)
  ResponseEntity<String> handleTest(@PathVariable("jobId") String jobId) {

    // before save in S3

    // send to rabbitmq
    final ProcessAudioCommand processAudioCommand =
        ProcessAudioCommand.newBuilder()
            .setJobId(jobId)
            .setUserId("jan")
            .setInputS3Bucket("bucket.input")
            .setInputS3Key("input.key")
            .setOutputS3Bucket("bucket.output")
            .setOutputS3Key("output.key")
            .build();
    messagePublisher.publish(
        "transcoder.commands",
        "audio.process",
        processAudioCommand,
        StandardSerializerFormat.PROTOBUF);

    return ResponseEntity.ok("Handled!");
  }
}
