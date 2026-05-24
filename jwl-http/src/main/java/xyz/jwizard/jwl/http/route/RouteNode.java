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
package xyz.jwizard.jwl.http.route;

import java.util.HashMap;
import java.util.Map;

class RouteNode {
    private final Map<String, RouteNode> children = new HashMap<>();
    private RouteNode variableChild = null;
    private String variableName = null;
    private Route route = null;

    Map<String, RouteNode> getStaticChildren() {
        return children;
    }

    RouteNode getVariableChild() {
        return variableChild;
    }

    void setVariableChild(RouteNode variableChild) {
        this.variableChild = variableChild;
    }

    String getVariableName() {
        return variableName;
    }

    void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    Route getRoute() {
        return route;
    }

    void setRoute(Route route) {
        this.route = route;
    }
}
