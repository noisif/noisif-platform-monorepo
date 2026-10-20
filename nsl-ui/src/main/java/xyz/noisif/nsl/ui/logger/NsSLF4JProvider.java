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

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import java.util.ServiceLoader;

public class NsSLF4JProvider implements SLF4JServiceProvider {
  private static final String REQUESTED_API_VERSION = "2.0.17";

  private ILoggerFactory loggerFactory;
  private IMarkerFactory markerFactory;
  private MDCAdapter mdcAdapter;

  @Override
  public ILoggerFactory getLoggerFactory() {
    return loggerFactory;
  }

  @Override
  public IMarkerFactory getMarkerFactory() {
    return markerFactory;
  }

  @Override
  public MDCAdapter getMDCAdapter() {
    return mdcAdapter;
  }

  @Override
  public String getRequestedApiVersion() {
    return REQUESTED_API_VERSION;
  }

  @Override
  @SuppressWarnings("SystemOut")
  public void initialize() {
    ConsoleAdapter resolvedAdapter = null;
    for (final ConsoleAdapter adapter : ServiceLoader.load(ConsoleAdapter.class)) {
      resolvedAdapter = adapter;
      break;
    }
    if (resolvedAdapter == null) {
      resolvedAdapter =
          (level, module, action, text) -> {
            String act = (action != null && !action.isEmpty()) ? "[" + action + "] " : "";
            System.out.println("[" + level + "] [" + module + "] " + act + "- " + text);
          };
    }
    final ConsoleAdapter finalAdapter = resolvedAdapter;
    loggerFactory = name -> new ColoredLogger(name, finalAdapter);
    markerFactory = new BasicMarkerFactory();
    mdcAdapter = new BasicMDCAdapter();
  }
}
