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
package xyz.jwizard.jwl.kv.jedis.pubsub.pattern;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xyz.jwizard.jwl.kv.pubsub.pattern.ChannelParamExtractor;

public class RegexChannelParamExtractor implements ChannelParamExtractor {
    private static final String[] EMPTY_PARAMS = new String[0];

    private final Pattern compiledPattern;

    public RegexChannelParamExtractor(String redisPattern) {
        if (redisPattern != null && redisPattern.contains("*")) {
            final String regexString = "\\Q" + redisPattern.replace("*", "\\E(.*)\\Q") + "\\E";
            this.compiledPattern = Pattern.compile(regexString);
        } else {
            this.compiledPattern = null;
        }
    }

    @Override
    public String[] extract(String channel) {
        if (compiledPattern == null || channel == null) {
            return EMPTY_PARAMS;
        }
        final Matcher matcher = compiledPattern.matcher(channel);
        if (matcher.matches()) {
            final int groupCount = matcher.groupCount();
            final String[] params = new String[groupCount];
            for (int i = 0; i < groupCount; i++) {
                params[i] = matcher.group(i + 1);
            }
            return params;
        }
        return EMPTY_PARAMS;
    }
}
