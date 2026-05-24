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
package xyz.jwizard.jwl.queue;

import xyz.jwizard.jwl.codec.serialization.StandardSerializerFormat;

public class FailingListener implements QueueListener<byte[]> {
    @Override
    public String getQueueName() {
        return "test.fail.queue";
    }

    @Override
    public Class<byte[]> getMessageType() {
        return byte[].class;
    }

    @Override
    public StandardSerializerFormat getFormat() {
        return StandardSerializerFormat.RAW;
    }

    @Override
    public QueueTopology getTopology() {
        return QueueTopology.builder()
            .withDeadLetter()
            .build();
    }

    @Override
    public void onMessage(byte[] message) {
        throw new RuntimeException("Simulated processing failure");
    }
}
