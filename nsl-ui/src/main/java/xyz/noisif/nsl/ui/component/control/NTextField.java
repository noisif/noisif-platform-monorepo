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
package xyz.noisif.nsl.ui.component.control;

import xyz.noisif.nsl.ui.component.NComponent;
import xyz.noisif.nsl.ui.core.UiContext;
import xyz.noisif.nsl.ui.rx.Observable;
import xyz.noisif.nsl.ui.rx.State;
import xyz.noisif.nsl.ui.spi.control.TextFieldPeer;
import xyz.noisif.nsl.ui.validation.Validatable;
import xyz.noisif.nsl.ui.validation.ValidationRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class NTextField extends NComponent implements Validatable {
  private final List<ValidationRule> rules = new ArrayList<>();
  private final State<String> textState = new State<>("");
  private final State<Boolean> notBlankState = new State<>(false);

  public NTextField(String id, String placeholder) {
    this(id, placeholder, false);
  }

  public NTextField(String id, String placeholder, boolean isPassword) {
    super(UiContext.toolkit().createTextField());
    setId(id);
    setPlaceholder(placeholder);
    setPasswordMode(isPassword);
    ((TextFieldPeer) peer)
        .setOnChange(
            () -> {
              String current = ((TextFieldPeer) peer).getValue();
              textState.set(current);
              notBlankState.set(current != null && !current.isBlank());
            });
  }

  public Observable<String> textProperty() {
    return textState;
  }

  public Observable<Boolean> notBlankProperty() {
    return notBlankState;
  }

  public NTextField addValidators(ValidationRule... validationRules) {
    this.rules.addAll(Arrays.asList(validationRules));
    return this;
  }

  public void setPlaceholder(String placeholder) {
    ((TextFieldPeer) peer).setPlaceholder(placeholder);
  }

  public void setPasswordMode(boolean isPassword) {
    ((TextFieldPeer) peer).setPasswordMode(isPassword);
  }

  public String getText() {
    return ((TextFieldPeer) peer).getValue();
  }

  public void setText(String text) {
    ((TextFieldPeer) peer).setValue(text);
    textState.set(text != null ? text : "");
    notBlankState.set(text != null && !text.isBlank());
  }

  public void setError(String errorMessage) {
    ((TextFieldPeer) peer).setError(errorMessage);
  }

  public void clearError() {
    ((TextFieldPeer) peer).setError(null);
  }

  public void setOnChange(Runnable action) {
    ((TextFieldPeer) peer).setOnChange(action);
  }

  @Override
  public boolean validate() {
    final String currentValue = getText();
    for (final ValidationRule rule : rules) {
      final Optional<String> error = rule.validate(currentValue);
      if (error.isPresent()) {
        setError(error.get());
        return false;
      }
    }
    clearError();
    return true;
  }

  @Override
  public void clearValidation() {
    clearError();
  }
}
