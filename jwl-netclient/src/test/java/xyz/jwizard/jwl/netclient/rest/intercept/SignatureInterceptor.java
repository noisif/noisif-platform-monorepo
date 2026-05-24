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
package xyz.jwizard.jwl.netclient.rest.intercept;

import xyz.jwizard.jwl.netclient.rest.TestHttpHeaderName;
import xyz.jwizard.jwl.netclient.rest.TestHttpHeaderValue;

public class SignatureInterceptor implements RequestInterceptor {
    @Override
    public void intercept(InterceptorContext context) {
        final RequestView view = context.getView();
        final String actionType = view.getHeaders().get(TestHttpHeaderName.X_ACTION_TYPE.getCode());
        final String targetId = view.getQueryParams().get("target_id");
        if (actionType != null && targetId != null) {
            context.addHeader(TestHttpHeaderName.X_REQUEST_SIGNATURE, TestHttpHeaderValue.SIG,
                actionType, targetId);
        }
    }

    @Override
    public int order() {
        return 100;
    }
}
