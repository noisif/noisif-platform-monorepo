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
package xyz.jwizard.jwl.net;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NetworkUtilTest {
    @Test
    @DisplayName("should parse valid host:port string into HostPort object")
    void shouldParseValidAddressCorrectly() {
        // given
        final String address = "localhost:8080";
        // when
        final HostPort result = NetworkUtil.parseHostPort(address);
        // then
        assertNotNull(result, "Parsed HostPort object should not be null");
        assertEquals("localhost", result.host(), "Parsed host should match the input");
        assertEquals(8080, result.port(), "Parsed port should match the input");
    }

    @Test
    @DisplayName("should trim whitespaces and parse valid host:port string")
    void shouldTrimWhitespacesAndParseValidAddress() {
        // given
        final String address = "  127.0.0.1  :  9090  ";
        // when
        final HostPort result = NetworkUtil.parseHostPort(address);
        // then
        assertNotNull(result, "Parsed HostPort object should not be null");
        assertEquals("127.0.0.1", result.host(), "Host should be correctly trimmed");
        assertEquals(9090, result.port(), "Port should be correctly parsed after trimming");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when address is null")
    void shouldThrowExceptionWhenAddressIsNull() {
        // when & then
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> NetworkUtil.parseHostPort(null),
            "Should throw IllegalArgumentException for null address");
        assertEquals("Address string cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when address is blank")
    void shouldThrowExceptionWhenAddressIsBlank() {
        // given
        final String blankAddress = "   ";
        // when & then
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> NetworkUtil.parseHostPort(blankAddress),
            "Should throw IllegalArgumentException for blank address");
        assertEquals("Address string cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when address is missing a colon")
    void shouldThrowExceptionWhenAddressIsMissingColon() {
        // given
        final String address = "localhost8080";
        // when & then
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> NetworkUtil.parseHostPort(address),
            "Should throw exception when format does not contain a colon");
        assertTrue(exception.getMessage().contains("Invalid address format"),
            "Exception message should indicate invalid format");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when address has multiple colons")
    void shouldThrowExceptionWhenAddressHasTooManyColons() {
        // given
        final String address = "localhost:8080:9090";
        // when & then
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> NetworkUtil.parseHostPort(address),
            "Should throw exception when format contains more than one colon");
        assertTrue(exception.getMessage().contains("Invalid address format"),
            "Exception message should indicate invalid format");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when port is not a valid integer")
    void shouldThrowExceptionWhenPortIsNotANumber() {
        // given
        final String address = "localhost:abc";
        // when & then
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> NetworkUtil.parseHostPort(address),
            "Should throw exception when port part cannot be parsed to an integer");
        assertTrue(exception.getMessage().contains("Invalid port number"),
            "Exception message should indicate invalid port number");
        assertNotNull(exception.getCause(), "Exception should contain the original cause");
        assertEquals(NumberFormatException.class, exception.getCause().getClass(),
            "Root cause should be NumberFormatException");
    }

    @Test
    @DisplayName("should add query parameter to URI without existing parameters")
    void shouldAddParameterToSimpleUri() {
        // given
        final String uri = "https://jwizard.xyz/ws";
        // when
        final String result = NetworkUtil.addQueryParameter(uri, "token", "secret");
        // then
        assertEquals("https://jwizard.xyz/ws?token=secret", result);
    }

    @Test
    @DisplayName("should append query parameter to URI that already has parameters")
    void shouldAppendParameterToExistingQuery() {
        // given
        final String uri = "https://jwizard.xyz/ws?v=10";
        // when
        final String result = NetworkUtil.addQueryParameter(uri, "encoding", "etf");
        // then
        assertEquals("https://jwizard.xyz/ws?v=10&encoding=etf", result);
    }

    @Test
    @DisplayName("should correctly encode special characters in query parameters")
    void shouldEncodeSpecialCharacters() {
        // given
        final String uri = "https://jwizard.xyz/api";
        final String key = "user name";
        final String value = "Jan Kowalski + Co";
        // when
        final String result = NetworkUtil.addQueryParameter(uri, key, value);
        // then
        assertTrue(result.contains("user") && result.contains("name"), "Key should be present");
        assertTrue(result.contains("Jan") && result.contains("Kowalski"),
            "Value should be present");
        assertTrue(result.contains("%2B"), "The plus sign must be encoded as %2B");
        assertTrue(result.startsWith("https://jwizard.xyz/api?"),
            "Should start with correctly appended ?");
    }

    @Test
    @DisplayName("should insert query parameter before URI fragment (anchor)")
    void shouldHandleUriWithFragment() {
        // given
        final String uri = "https://jwizard.xyz/docs#section-1";
        // when
        final String result = NetworkUtil.addQueryParameter(uri, "auth", "true");
        // then
        assertEquals("https://jwizard.xyz/docs?auth=true#section-1", result);
    }

    @Test
    @DisplayName("should handle complex URIs with existing query and fragment")
    void shouldHandleComplexUriCorrectly() {
        // given
        final String uri = "wss://gateway.discord.gg/?v=10#resume";
        // when
        final String result = NetworkUtil.addQueryParameter(uri, "compress", "zlib-stream");
        // then
        assertEquals("wss://gateway.discord.gg/?v=10&compress=zlib-stream#resume", result);
    }

    @Test
    @DisplayName("should return original URI when key or value is null")
    void shouldReturnOriginalWhenInputsAreNull() {
        // given
        final String uri = "https://jwizard.xyz";
        // when & then
        assertEquals(uri, NetworkUtil.addQueryParameter(uri, null, "val"));
        assertEquals(uri, NetworkUtil.addQueryParameter(uri, "key", null));
    }

    @Test
    @DisplayName("should use fallback when URI is malformed")
    void shouldFallbackOnMalformedUri() {
        // given
        final String malformed = "not a valid uri ^";
        // when
        final String result = NetworkUtil.addQueryParameter(malformed, "a", "b");
        // then
        assertTrue(result.endsWith("?a=b") || result.endsWith("&a=b"));
    }

    @Test
    @DisplayName("should correctly parse query parameters from a valid URI")
    void shouldParseQueryParameters() {
        // given
        final String uri = "ws://localhost:8080/v1?encoding=raw&frame=binary&token=secret123";
        // when
        final Map<String, String> params = NetworkUtil.getQueryParameters(uri);
        // then
        assertThat(params).hasSize(3)
            .containsEntry("encoding", "raw")
            .containsEntry("frame", "binary")
            .containsEntry("token", "secret123");
    }

    @Test
    @DisplayName("should ignore fragment and return empty map for uri without parameters")
    void shouldHandleFragmentAndEmptyQuery() {
        // given
        final String uriWithFragment = "ws://localhost:8080/v1#section1";
        final String uriClean = "ws://localhost:8080/v1";
        // when
        final Map<String, String> paramsFragment = NetworkUtil.getQueryParameters(uriWithFragment);
        final Map<String, String> paramsClean = NetworkUtil.getQueryParameters(uriClean);
        // then
        assertThat(paramsFragment).isEmpty();
        assertThat(paramsClean).isEmpty();
    }
}
