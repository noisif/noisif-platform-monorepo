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
package xyz.jwizard.jwl.net.envelope.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.envelope.MessageEnvelope;
import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.common.Ordered;
import xyz.jwizard.jwl.common.di.ComponentProvider;
import xyz.jwizard.jwl.common.reflect.TypeReference;
import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.common.util.CastUtil;
import xyz.jwizard.jwl.net.bus.RawBusListener;
import xyz.jwizard.jwl.net.envelope.ActionGroup;
import xyz.jwizard.jwl.net.envelope.EnvelopeAction;
import xyz.jwizard.jwl.net.envelope.EnvelopeSession;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class EnvelopeBusListener<S extends EnvelopeSession> implements RawBusListener<S> {
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Map<Integer, Class<?>> typeRegistry = new ConcurrentHashMap<>();
  private final Map<Integer, EnvelopeAction<S, ?>> actionHandlers = new ConcurrentHashMap<>();

  private final ActionGroup actionGroup;
  private final ComponentProvider componentProvider;

  protected EnvelopeBusListener(AbstractBuilder<?, ?> builder) {
    actionGroup = builder.actionGroup;
    componentProvider = builder.componentProvider;
    registerEnvelopeActions();
  }

  protected void registerManually(EnvelopeAction<S, Void> action) {
    typeRegistry.put(action.opCode().getCode(), action.payloadClass());
    actionHandlers.put(action.opCode().getCode(), action);
    log.info("Successfully registered action: {} manually", action.getClass().getSimpleName());
  }

  private void registerEnvelopeActions() {
    final Collection<EnvelopeAction<S, ?>> actions =
        componentProvider.getInstancesOf(new TypeReference<>() {});
    int registeredCount = 0;
    for (final EnvelopeAction<S, ?> action : actions) {
      if (actionGroup.groupName().equals(action.group().groupName())) {
        registerAction(action);
        registeredCount++;
      }
    }
    log.info("Successfully auto-registered {} envelope action(s)", registeredCount);
  }

  private void registerAction(EnvelopeAction<S, ?> action) {
    final OpCode op = action.opCode();
    typeRegistry.put(op.getCode(), action.payloadClass());
    actionHandlers.put(op.getCode(), action);
    log.debug("Registered action: {} (OP: {})", action.getClass().getSimpleName(), op.getCode());
  }

  @Override
  public final void dispatch(S session, byte[] message) {
    try {
      processEnvelope(session, session.unwrap(message, typeRegistry::get));
    } catch (Exception ex) {
      handleProcessingError(session, ex);
    }
  }

  @Override
  public final void dispatch(S session, String message) {
    try {
      processEnvelope(session, session.unwrap(message, typeRegistry::get));
    } catch (Exception ex) {
      handleProcessingError(session, ex);
    }
  }

  private void processEnvelope(S session, MessageEnvelope<?> envelope) {
    final EnvelopeAction<S, Object> handler =
        CastUtil.unsafeCast(actionHandlers.get(envelope.op()));
    if (handler != null) {
      handler.handle(session, envelope.data());
      return;
    }
    handleProcessUnknownAction(session, envelope);
  }

  protected void handleProcessUnknownAction(S session, MessageEnvelope<?> envelope) {
    log.warn(
        "No action handler found for OP code: {} in session: {}",
        envelope.op(),
        session.getSessionId());
  }

  protected void handleProcessingError(S session, Exception ex) {
    log.error(
        "Error processing envelope from session {}: {}", session.getSessionId(), ex.getMessage());
  }

  protected abstract static class AbstractBuilder<
      S extends EnvelopeSession, B extends AbstractBuilder<S, B>> {
    private ActionGroup actionGroup = ActionGroup.GLOBAL;
    private Integer order = Ordered.HIGHEST_PRIORITY;
    private ComponentProvider componentProvider;

    protected abstract B self();

    public B actionGroup(ActionGroup actionGroup) {
      this.actionGroup = actionGroup;
      return self();
    }

    public B order(Integer order) {
      this.order = order;
      return self();
    }

    public B componentProvider(ComponentProvider componentProvider) {
      this.componentProvider = componentProvider;
      return self();
    }

    public void validate() {
      Assert.notNull(actionGroup, "ActionGroup cannot be null");
      Assert.notNull(order, "Order cannot be null");
      Assert.notNull(componentProvider, "ComponentProvider cannot be null");
    }

    public abstract EnvelopeBusListener<S> build();
  }
}
