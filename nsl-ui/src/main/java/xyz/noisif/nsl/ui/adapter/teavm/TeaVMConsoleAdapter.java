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
package xyz.noisif.nsl.ui.adapter.teavm;

import org.teavm.jso.JSBody;

import xyz.noisif.nsl.ui.logger.ConsoleAdapter;

public class TeaVMConsoleAdapter implements ConsoleAdapter {
  @Override
  public void logToConsole(String level, String module, String action, String text) {
    jsLogToConsole(level, module, action, text);
  }

  @JSBody(
      params = {"level", "module", "action", "text"},
      script =
          """
    const mStyle = 'color: magenta; font-weight: bold;';
    const dStyle = 'color: inherit; font-weight: normal;';
    const format = action
        ? '%c[' + module + ']%c [' + action + '] - ' + text
        : '%c[' + module + ']%c - ' + text;
    const method = level.toLowerCase();
    const logFn = console[method] ? method : 'info';
    console[logFn](format, mStyle, dStyle);
    """)
  private static native void jsLogToConsole(
      String level, String module, String action, String text);
}
