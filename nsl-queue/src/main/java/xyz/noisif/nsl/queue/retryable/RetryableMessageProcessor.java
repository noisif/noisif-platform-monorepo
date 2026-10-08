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
package xyz.noisif.nsl.queue.retryable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.util.io.RunnableWithException;
import xyz.noisif.nsl.queue.MessageAcknowledgment;
import xyz.noisif.nsl.queue.MessageProcessor;
import xyz.noisif.nsl.queue.QueueTopology;

public class RetryableMessageProcessor implements MessageProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(RetryableMessageProcessor.class);

  private final QueueTopology topology;
  private final String queueName;

  public RetryableMessageProcessor(QueueTopology topology, String queueName) {
    this.topology = topology;
    this.queueName = queueName;
  }

  @Override
  public void process(RunnableWithException task, MessageAcknowledgment acknowledgment) {
    final int maxAttempts = topology.maxRetries() + 1;
    int currentAttempt = 1;
    boolean success = false;
    while (currentAttempt <= maxAttempts) {
      try {
        task.run();
        success = true;
        break;
      } catch (FatalMessageProcessingException ex) {
        LOG.error(
            "Fatal error detected on queue '{}', aborting retries and routing straight to DLX",
            queueName,
            ex);
        break;
      } catch (Exception ex) {
        if (currentAttempt < maxAttempts) {
          LOG.warn(
              "Processing failed for message on queue '{}' (attempt {}/{}), retrying in {} ms...",
              queueName,
              currentAttempt,
              maxAttempts,
              topology.retryDelayMs());
          try {
            Thread.sleep(topology.retryDelayMs());
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOG.error("Retry sleep interrupted for queue '{}'", queueName);
            break;
          }
          currentAttempt++;
        } else {
          LOG.error(
              "Final failure after {} attempts for message on queue '{}'",
              maxAttempts,
              queueName,
              ex);
          break;
        }
      }
    }
    try {
      if (success) {
        acknowledgment.acknowledge();
      } else {
        acknowledgment.reject();
      }
    } catch (Exception ex) {
      LOG.error(
          "Critical error while executing acknowledgment/rejection for queue '{}'", queueName, ex);
    }
  }
}
