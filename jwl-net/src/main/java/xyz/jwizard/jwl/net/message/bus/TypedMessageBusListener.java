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
package xyz.jwizard.jwl.net.message.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.net.bus.RawBusListener;
import xyz.jwizard.jwl.net.message.RawMessageSession;

public abstract class TypedMessageBusListener<T, S extends RawMessageSession>
    implements RawBusListener<S> {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void dispatch(S session, byte[] message) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("Received raw binary message ({} bytes) in session {}",
                    message.length, session.getSessionId());
            }
            final T parsedMessage = session.parse(message, getTargetType());
            if (log.isDebugEnabled()) {
                log.debug("Successfully parsed binary message to {} in session {}",
                    getTargetType().getSimpleName(), session.getSessionId());
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
                log.trace("Received raw text message ({} chars) in session {}",
                    message.length(), session.getSessionId());
            }
            final T parsedMessage = session.parse(message, getTargetType());
            if (log.isDebugEnabled()) {
                log.debug("Successfully parsed text message to {} in session {}",
                    getTargetType().getSimpleName(), session.getSessionId());
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
