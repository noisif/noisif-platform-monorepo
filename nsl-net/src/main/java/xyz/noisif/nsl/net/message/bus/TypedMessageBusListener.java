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
package xyz.noisif.nsl.net.message.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.net.bus.RawBusListener;
import xyz.noisif.nsl.net.message.RawMessageSession;

public abstract class TypedMessageBusListener<T, S extends RawMessageSession>
    implements RawBusListener<S> {
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public void dispatch(S session, byte[] message) {
    try {
      if (log.isTraceEnabled()) {
        log.trace(
            "Received raw binary message ({} bytes) in session {}",
            message.length,
            session.getSessionId());
      }
      final T parsedMessage = session.parse(message, getTargetType());
      if (log.isDebugEnabled()) {
        log.debug(
            "Successfully parsed binary message to {} in session {}",
            getTargetType().getSimpleName(),
            session.getSessionId());
      }
      handle(session, parsedMessage);
    } catch (Exception ex) {
      handleError(session, ex);
    }
  }

  @Override
  public void dispatch(S session, String message) {
    try {
      if (log.isTraceEnabled()) {
        log.trace(
            "Received raw text message ({} chars) in session {}",
            message.length(),
            session.getSessionId());
      }
      final T parsedMessage = session.parse(message, getTargetType());
      if (log.isDebugEnabled()) {
        log.debug(
            "Successfully parsed text message to {} in session {}",
            getTargetType().getSimpleName(),
            session.getSessionId());
      }
      handle(session, parsedMessage);
    } catch (Exception ex) {
      handleError(session, ex);
    }
  }

  protected abstract Class<T> getTargetType();

  protected abstract void handle(S session, T message);

  protected void handleError(S session, Exception ex) {
    log.error("Failed to parse RAW message in session {}", session.getSessionId(), ex);
  }
}
