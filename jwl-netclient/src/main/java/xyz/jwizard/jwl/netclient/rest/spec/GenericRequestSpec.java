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
package xyz.jwizard.jwl.netclient.rest.spec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.jwizard.jwl.codec.serialization.MessageSerializer;
import xyz.jwizard.jwl.codec.serialization.SerializerFormat;
import xyz.jwizard.jwl.codec.serialization.SerializerRegistry;
import xyz.jwizard.jwl.common.Ordered;
import xyz.jwizard.jwl.common.retry.RetryExecutor;
import xyz.jwizard.jwl.common.util.CastUtil;
import xyz.jwizard.jwl.common.util.CollectionUtil;
import xyz.jwizard.jwl.net.NetworkUtil;
import xyz.jwizard.jwl.net.http.HttpMethod;
import xyz.jwizard.jwl.net.http.HttpStatus;
import xyz.jwizard.jwl.net.http.auth.AuthScheme;
import xyz.jwizard.jwl.net.http.header.HttpHeaderName;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.group.ClientRegistry;
import xyz.jwizard.jwl.netclient.rest.RestRequestException;
import xyz.jwizard.jwl.netclient.rest.RestResponse;
import xyz.jwizard.jwl.netclient.rest.group.RestClientGroupConfig;
import xyz.jwizard.jwl.netclient.rest.intercept.InterceptorContext;
import xyz.jwizard.jwl.netclient.rest.intercept.RequestInterceptor;
import xyz.jwizard.jwl.netclient.rest.intercept.RequestView;
import xyz.jwizard.jwl.netclient.rest.retry.RetryPolicy;

public abstract class GenericRequestSpec implements RequestSpec, RequestView {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final ClientRegistry<RestClientGroupConfig> clientRegistry;
    protected final String url;
    protected final HttpMethod method;
    protected final SerializerRegistry<MessageSerializer> serializerRegistry;

    protected final List<RequestInterceptor> interceptors = new ArrayList<>();
    protected final Map<String, String> headers = new LinkedHashMap<>();
    protected final Map<String, String> queryParams = new LinkedHashMap<>();
    protected final Map<String, String> formParams = new LinkedHashMap<>();

    protected ClientGroup clientGroup;
    protected MessageSerializer messageSerializer;
    protected Object body;
    protected Duration requestTimeout;
    protected RestResponse<?> abortedResponse;

    protected final InterceptorContext reusableContext = new InterceptorContext() {
        @Override
        public RequestView getView() {
            return GenericRequestSpec.this;
        }

        @Override
        public void addUnsafeHeader(HttpHeaderName name, String value) {
            GenericRequestSpec.this.unsafeHeader(name, value);
        }

        @Override
        public void addQueryParam(String name, String value) {
            GenericRequestSpec.this.queryParam(name, value);
        }

        @Override
        public void setAuth(AuthScheme scheme, String... credentials) {
            GenericRequestSpec.this.auth(scheme, credentials);
        }

        @Override
        public void abortWith(RestResponse<?> response) {
            GenericRequestSpec.this.abortedResponse = response;
        }
    };
    protected RetryPolicy requestRetryPolicy;
    private boolean localInterceptorsSorted = true;
    private boolean serializerOverridden = false;

    protected GenericRequestSpec(ClientRegistry<RestClientGroupConfig> clientRegistry,
                                 String url, HttpMethod method,
                                 SerializerRegistry<MessageSerializer> serializerRegistry) {
        this.clientRegistry = clientRegistry;
        this.url = url;
        this.method = method;
        this.serializerRegistry = serializerRegistry;
        clientGroup = ClientGroup.GLOBAL;
        messageSerializer = updateSerializerFromPool(clientGroup);
        requestRetryPolicy = clientRegistry.get(clientGroup).getRetryPolicy();
    }

    @Override
    public RequestSpec group(ClientGroup clientGroup) {
        this.clientGroup = clientGroup;
        if (!serializerOverridden) {
            messageSerializer = updateSerializerFromPool(clientGroup);
        }
        if (requestRetryPolicy == null) {
            requestRetryPolicy = clientRegistry.get(clientGroup).getRetryPolicy();
        }
        return this;
    }

    @Override
    public RequestSpec unsafeHeader(HttpHeaderName name, String value) {
        headers.put(name.getCode(), value);
        return this;
    }

    @Override
    public RequestSpec queryParam(String name, String value) {
        queryParams.put(name, value);
        return this;
    }

    @Override
    public RequestSpec formParam(String name, String value) {
        formParams.put(name, value);
        return this;
    }

    @Override
    public RequestSpec body(Object body) {
        this.body = body;
        return this;
    }

    @Override
    public RequestSpec serializer(SerializerFormat format) {
        messageSerializer = serializerRegistry.get(format);
        serializerOverridden = true;
        return this;
    }

    @Override
    public RequestSpec timeout(Duration timeout) {
        this.requestTimeout = timeout;
        return this;
    }

    @Override
    public RequestSpec interceptor(RequestInterceptor interceptor) {
        interceptors.add(interceptor);
        localInterceptorsSorted = false;
        return this;
    }

    @Override
    public RequestSpec retry(int maxRetries, Duration backoffMs) {
        requestRetryPolicy = RetryPolicy.withSafeMethods(maxRetries + 1, backoffMs);
        return this;
    }

    @Override
    public RequestSpec retry(int maxRetries, Duration backoffMs, Duration maxBackoffMs) {
        requestRetryPolicy = RetryPolicy.withSafeMethods(maxRetries + 1, backoffMs, maxBackoffMs);
        return this;
    }

    @Override
    public RequestSpec disableRetry() {
        requestRetryPolicy = RetryPolicy.none();
        return this;
    }

    @Override
    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public ClientGroup getGroup() {
        return clientGroup;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    @Override
    public Map<String, String> getFormParams() {
        return formParams;
    }

    @Override
    public Object getBody() {
        return body;
    }

    @Override
    public final <T> RestResponse<T> send(Class<T> responseType) {
        abortedResponse = null;
        List<RequestInterceptor> groupInterceptors = List.of();
        if (clientGroup != null) {
            groupInterceptors = clientRegistry.get(clientGroup).getInterceptors();
        }
        if (!localInterceptorsSorted && !interceptors.isEmpty()) {
            interceptors.sort(Ordered.COMPARATOR);
            localInterceptorsSorted = true;
        }
        log.trace("Executing interceptors for {} {} (group: {})", method, url,
            clientGroup != null ? clientGroup.getClientGroupName() : "none");
        CollectionUtil.consumeMergedSorted(
            groupInterceptors,
            interceptors,
            Ordered.COMPARATOR,
            interceptor -> {
                log.trace("Running interceptor: {}", interceptor.getClass().getSimpleName());
                interceptor.intercept(reusableContext);
                if (abortedResponse != null) {
                    log.debug("Request aborted by interceptor: {} for {} {}",
                        interceptor.getClass().getSimpleName(), method, url);
                }
                return abortedResponse == null;
            }
        );
        if (abortedResponse != null) {
            return CastUtil.unsafeCast(abortedResponse);
        }
        try {
            log.debug("Starting request execution: {} {} (retry policy: {})", method, url,
                requestRetryPolicy.getClass().getSimpleName());
            return RetryExecutor.executeSync(
                () -> onSend(responseType),
                method,
                requestRetryPolicy,
                (attempt, response) -> {
                    final boolean retryable = isRetryableStatus(response.getStatus());
                    if (retryable) {
                        log.debug("Received retryable status code: {} for {} {}",
                            response.getStatus(), method, url);
                    }
                    return retryable;
                },
                (attempt, ex) -> {
                    log.debug("Request exception encountered (attempt {}): {}", attempt,
                        ex.getMessage());
                    return true; // retryableErr
                }
            );
        } catch (Exception ex) {
            if (ex instanceof RestRequestException restRequestEx) {
                throw restRequestEx;
            }
            log.error("Request failed definitively: {} {} - {}", method, url, ex.getMessage());
            throw new RestRequestException(String.format("Request failed: %s %s", method.name(),
                url), ex);
        }
    }

    protected abstract <T> RestResponse<T> onSend(Class<T> responseType);

    protected String resolveFullUrl() {
        if (NetworkUtil.isAbsoluteUrl(url)) {
            return url;
        }
        final String baseUrl = clientRegistry.get(clientGroup).getUrl();
        return NetworkUtil.concatPaths(baseUrl, url);
    }

    protected <T> T parseResponseBody(byte[] responseBytes, Class<T> responseType) {
        T parsedBody = null;
        if (responseType != Void.class && responseBytes != null && responseBytes.length > 0) {
            parsedBody = messageSerializer.deserializeFromBytes(responseBytes, responseType);
        }
        return parsedBody;
    }

    private MessageSerializer updateSerializerFromPool(ClientGroup clientGroup) {
        return serializerRegistry.get(clientRegistry.get(clientGroup).getDefaultFormat());
    }

    private boolean isRetryableStatus(HttpStatus status) {
        final int code = status.getCode();
        return code >= 500 || code == 429;
    }
}
