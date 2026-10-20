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
package xyz.noisif.nsl.ui.logger;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import java.io.Serial;

public class ColoredLogger extends LegacyAbstractLogger {
  @Serial private static final long serialVersionUID = 1L;

  private final transient ConsoleAdapter consoleAdapter;

  public ColoredLogger(String name, ConsoleAdapter consoleAdapter) {
    this.name = name;
    this.consoleAdapter = consoleAdapter;
  }

  @Override
  protected String getFullyQualifiedCallerName() {
    return null;
  }

  @Override
  protected void handleNormalizedLoggingCall(
      Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
    String formattedMsg = MessageFormatter.arrayFormat(messagePattern, arguments).getMessage();
    if (throwable != null) {
      formattedMsg += "\n" + throwable;
    }
    final String action = marker != null ? marker.getName() : null;
    final String shortModule = name.substring(name.lastIndexOf('.') + 1);
    consoleAdapter.logToConsole(level.name(), shortModule, action, formattedMsg);
  }

  @Override
  public boolean isTraceEnabled() {
    return false;
  }

  @Override
  public boolean isDebugEnabled() {
    return true;
  }

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public boolean isWarnEnabled() {
    return true;
  }

  @Override
  public boolean isErrorEnabled() {
    return true;
  }
}
