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
package xyz.noisif.nss.registry.ui.view;

import xyz.noisif.nsl.ui.SpaEntrypoint;
import xyz.noisif.nsl.ui.auth.AuthSession;
import xyz.noisif.nsl.ui.component.NFrame;
import xyz.noisif.nsl.ui.component.alert.AlertType;
import xyz.noisif.nsl.ui.component.alert.NAlertBanner;
import xyz.noisif.nsl.ui.component.container.Align;
import xyz.noisif.nsl.ui.component.container.ContainerSize;
import xyz.noisif.nsl.ui.component.container.Justify;
import xyz.noisif.nsl.ui.component.container.NConstraintBox;
import xyz.noisif.nsl.ui.component.container.NForm;
import xyz.noisif.nsl.ui.component.container.NHBox;
import xyz.noisif.nsl.ui.component.container.Padding;
import xyz.noisif.nsl.ui.component.container.Spacing;
import xyz.noisif.nsl.ui.component.control.NButton;
import xyz.noisif.nsl.ui.component.control.NTextField;
import xyz.noisif.nsl.ui.component.text.HeadingLevel;
import xyz.noisif.nsl.ui.component.text.NHeading;
import xyz.noisif.nsl.ui.fragment.NFooter;
import xyz.noisif.nsl.ui.http.HttpRequest;
import xyz.noisif.nsl.ui.http.JsonPayload;
import xyz.noisif.nsl.ui.rx.Bindings;
import xyz.noisif.nsl.ui.validation.Validators;
import xyz.noisif.nss.registry.ui.NssRegistryUiMain;

public class LoginView extends NConstraintBox {
  @Override
  protected void initLayout() {
    setSize(ContainerSize.XXL);
    setSpacing(Spacing.XXL);
    setPadding(Padding.XXL);
    setAlign(Align.CENTER);
    setJustify(Justify.CENTER);

    add(new LoginFrame(), new NFooter());
  }

  private static class LoginFrame extends NFrame {
    LoginFrame() {
      super("NSS Registry Panel");
    }

    @Override
    protected void initLayout() {
      add(new LoginForm());
    }
  }

  private static class LoginForm extends NForm {
    private final NAlertBanner alertBanner = new NAlertBanner(AlertType.DANGER);
    private final NTextField usernameField =
        new NTextField("login-username", "Username")
            .addValidators(Validators.required("Username cannot be empty"));

    private final NTextField passwordField =
        new NTextField("login-password", "Password", true)
            .addValidators(Validators.required("Password is required"));

    private final NButton submitButton = new NButton("Sign In");

    @Override
    protected void initLayout() {
      setSpacing(Spacing.MD);
      alertBanner.hide();
      submitButton.bindEnabled(
          Bindings.allTrue(usernameField.notBlankProperty(), passwordField.notBlankProperty()));
      add(
          new NHeading(HeadingLevel.H3, "Sign in"),
          alertBanner,
          usernameField,
          passwordField,
          new SubmitButtonRow(submitButton));
    }

    @Override
    protected void onValidSubmit() {
      submitButton.setLoading(true);
      alertBanner.hide();

      final JsonPayload loginData =
          JsonPayload.create()
              .put("username", usernameField.getText())
              .put("password", passwordField.getText());

      NssRegistryUiMain.API_V1.execute(
          HttpRequest.post("/auth/login").jsonBody(loginData),
          response -> {
            submitButton.setLoading(false);
            AuthSession.setAuthenticated(true);
            SpaEntrypoint.router().navigate("/");
          },
          errorResponse -> {
            submitButton.setLoading(false);
            alertBanner.showAlert(
                AlertType.DANGER, "Invalid credentials (" + errorResponse.status() + ")");
          },
          networkError -> {
            submitButton.setLoading(false);
            alertBanner.showAlert(AlertType.DANGER, networkError);
          });
    }

    @Override
    protected void onValidationFailed() {
      alertBanner.showAlert(AlertType.DANGER, "Please fix the validation errors below.");
    }
  }

  private static class SubmitButtonRow extends NHBox {
    private final NButton button;

    SubmitButtonRow(NButton button) {
      this.button = button;
    }

    @Override
    protected void initLayout() {
      setJustify(Justify.END);
      add(button);
    }
  }
}
