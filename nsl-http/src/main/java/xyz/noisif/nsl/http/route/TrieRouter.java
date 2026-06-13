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
package xyz.noisif.nsl.http.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.util.StringUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrieRouter implements Router {
  private static final Logger LOG = LoggerFactory.getLogger(TrieRouter.class);

  private static final String DELIMITER_START = "{";
  private static final String DELIMITER_END = "}";

  private final RouteNode root = new RouteNode();

  @Override
  public void addRoute(String method, String path, Route route) {
    final List<String> parts = StringUtil.split(method + path, '/');
    RouteNode current = root;
    for (final String part : parts) {
      if (part.isEmpty()) {
        continue;
      }
      if (part.startsWith(DELIMITER_START) && part.endsWith(DELIMITER_END)) {
        if (current.getVariableChild() == null) {
          current.setVariableName(part.substring(1, part.length() - 1));
          current.setVariableChild(new RouteNode());
        }
        current = current.getVariableChild();
      } else {
        current.getStaticChildren().putIfAbsent(part, new RouteNode());
        current = current.getStaticChildren().get(part);
      }
    }
    if (current.getRoute() != null) {
      LOG.warn("Overwriting existing route: {} {}", method, path);
    }
    current.setRoute(route);
    LOG.info("Registered route: {} {}", method, path);
  }

  @Override
  public MatchResult findRoute(String method, String path) {
    LOG.debug("Searching for route match: {} {}", method, path);
    final List<String> parts = StringUtil.split(method + path, '/');
    final Map<String, String> extractedVariables = new HashMap<>();
    RouteNode current = root;
    for (final String part : parts) {
      if (part.isEmpty()) {
        continue;
      }
      if (current.getStaticChildren().containsKey(part)) {
        current = current.getStaticChildren().get(part);
      } else if (current.getVariableChild() != null) {
        extractedVariables.put(current.getVariableName(), part);
        current = current.getVariableChild();
      } else {
        LOG.debug("Route not found (missed at node '{}'): {} {}", part, method, path);
        return null;
      }
    }
    if (current.getRoute() == null) {
      LOG.debug("Node exists but no route action assigned for: {} {}", method, path);
      return null;
    }
    LOG.debug("Route found for: {} {}, extracted variables: {}", method, path, extractedVariables);
    return new MatchResult(current.getRoute(), extractedVariables);
  }

  @Override
  public Set<String> getVariableNamesFor(String method, String path) {
    LOG.debug("Extracting variable names for validation: {} {}", method, path);
    final List<String> parts = StringUtil.split(method + path, '/');
    final Set<String> variableNames = new HashSet<>();
    RouteNode current = root;
    for (final String part : parts) {
      if (part.isEmpty()) {
        continue;
      }
      if (part.startsWith(DELIMITER_START) && part.endsWith(DELIMITER_END)) {
        final String varName = part.substring(1, part.length() - 1);
        variableNames.add(varName);
        LOG.debug("Found variable placeholder '{}' in path", varName);
        current = current.getVariableChild();
      } else {
        current = current.getStaticChildren().get(part);
      }
      if (current == null) {
        LOG.debug("Traversal stopped: part '{}' does not exist in trie", part);
        break;
      }
    }
    LOG.debug("Extracted variable names for {} {}: {}", method, path, variableNames);
    return variableNames;
  }
}
