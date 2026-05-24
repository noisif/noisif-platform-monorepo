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
package xyz.jwizard.jwl.common.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import xyz.jwizard.jwl.common.bootstrap.ForbiddenInstantiationException;

public class StringUtil {
    private StringUtil() {
        throw new ForbiddenInstantiationException(StringUtil.class);
    }

    public static String toLowerCase(String str) {
        if (str == null) {
            return null;
        }
        return str.toLowerCase(Locale.ROOT);
    }

    public static String toUpperCase(String str) {
        if (str == null) {
            return null;
        }
        return str.toUpperCase(Locale.ROOT);
    }

    // splits by char using fast indexOf() to avoid regex overhead, unpredictable trim patterns
    public static List<String> split(String str, char separator) {
        if (str == null) {
            return new ArrayList<>();
        }
        final List<String> result = new ArrayList<>();
        int start = 0;
        int nextSeparator;
        while ((nextSeparator = str.indexOf(separator, start)) != -1) {
            result.add(str.substring(start, nextSeparator));
            start = nextSeparator + 1;
        }
        result.add(str.substring(start));
        return result;
    }

    public static String splitAndGetFirst(String str, char separator) {
        final List<String> results = split(str, separator);
        if (results.isEmpty()) {
            return null;
        }
        return results.getFirst();
    }

    public static byte[] getBytes(String str) {
        if (str == null) {
            return new byte[0];
        }
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static String truncateToUtf8Bytes(String text, int maxBytes) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return text;
        }
        final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.IGNORE)
            .onUnmappableCharacter(CodingErrorAction.IGNORE);
        try {
            final ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, maxBytes);
            final CharBuffer decoded = decoder.decode(buffer);
            return decoded.toString();
        } catch (CharacterCodingException ex) {
            return "";
        }
    }
}
