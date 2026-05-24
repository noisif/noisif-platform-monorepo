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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import xyz.jwizard.jwl.common.limit.RateLimiter;
import xyz.jwizard.jwl.net.http.HttpStatus;
import xyz.jwizard.jwl.netclient.group.ClientGroup;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {
    @Mock
    private RateLimiter rateLimiter;
    @Mock
    private InterceptorContext context;
    @Mock
    private RequestView view;
    @Mock
    private ClientGroup clientGroup;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(rateLimiter);
        lenient().when(context.getView()).thenReturn(view);
        lenient().when(view.getGroup()).thenReturn(clientGroup);
        lenient().when(clientGroup.getClientGroupName()).thenReturn("DISCORD_API");
    }

    @Test
    @DisplayName("should let request pass when rate limit is not exceeded")
    void shouldLetRequestPass() {
        // given
        when(rateLimiter.tryAcquire("DISCORD_API")).thenReturn(true);
        // when
        interceptor.intercept(context);
        // then
        verify(context, never()).abortWith(any());
    }

    @Test
    @DisplayName("should abort request with 429 when rate limit is exceeded")
    void shouldAbortWhenRateLimitExceeded() {
        // given
        when(rateLimiter.tryAcquire("DISCORD_API")).thenReturn(false);
        // when
        interceptor.intercept(context);
        // then
        verify(context).abortWith(argThat(response -> response.getStatus()
            .equals(HttpStatus.TOO_MANY_REQUESTS_429) && response.getBody() == null
        ));
    }
}
