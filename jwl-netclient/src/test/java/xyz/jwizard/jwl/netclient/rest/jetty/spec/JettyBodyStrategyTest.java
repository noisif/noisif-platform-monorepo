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
package xyz.jwizard.jwl.netclient.rest.jetty.spec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.jwizard.jwl.netclient.rest.spec.GenericRequestSpec;

class JettyBodyStrategyTest {
    private final JettyBodyStrategy rawStrategy = new JettyRawBodyStrategy();
    private final JettyBodyStrategy formStrategy = new JettyFormBodyStrategy();

    @Test
    @DisplayName("raw strategy should support requests with raw body and no form params")
    void rawStrategyShouldSupportRawBody() {
        // given
        final GenericRequestSpec spec = mock(GenericRequestSpec.class);
        when(spec.getBody()).thenReturn("Some Raw JSON");
        when(spec.getFormParams()).thenReturn(null);
        // when & then
        assertThat(rawStrategy.supports(spec)).isTrue();
        assertThat(formStrategy.supports(spec)).isFalse();
    }

    @Test
    @DisplayName("form strategy should support requests with form params")
    void formStrategyShouldSupportFormParams() {
        // given
        final GenericRequestSpec spec = mock(GenericRequestSpec.class);
        when(spec.getBody()).thenReturn(null);
        when(spec.getFormParams()).thenReturn(Map.of("grant_type", "client_credentials"));
        // when & then
        assertThat(formStrategy.supports(spec)).isTrue();
        assertThat(rawStrategy.supports(spec)).isFalse();
    }
}
