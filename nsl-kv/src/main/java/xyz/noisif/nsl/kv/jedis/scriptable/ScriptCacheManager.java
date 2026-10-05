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
package xyz.noisif.nsl.kv.jedis.scriptable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.cache.ProviderCache;
import xyz.noisif.nsl.common.util.CryptoUtil;
import xyz.noisif.nsl.common.util.PathUtil;
import xyz.noisif.nsl.common.util.io.IoUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ScriptCacheManager {
  private static final Logger log = LoggerFactory.getLogger(ScriptCacheManager.class);

  private final ProviderCache<KvScript, KvScript, LoadedScript> scriptCache;
  private final String basePath;

  private ScriptCacheManager(String basePath, Set<KvScript> availableScripts) throws IOException {
    this.basePath = basePath;
    log.info(
        "Initializing ScriptCacheManager with {} scripts (base path: '{}')",
        availableScripts.size(),
        basePath);

    final Set<LoadedScript> loadedScripts = new HashSet<>();
    for (final KvScript script : availableScripts) {
      loadedScripts.add(loadScriptContentAndHash(script));
    }
    scriptCache =
        new ProviderCache<>(
            loadedScripts, (loadedScript, contextKey) -> loadedScript.key().equals(contextKey));
    log.info("Successfully loaded and cached {} scripts", loadedScripts.size());
  }

  public static ScriptCacheManager create(String basePath, Set<KvScript> availableScripts)
      throws IOException {
    return new ScriptCacheManager(basePath, availableScripts);
  }

  public LoadedScript getCachedScript(KvScript kvScript) {
    log.debug("Fetching cached script for key: {}", kvScript);
    final LoadedScript script = scriptCache.get(kvScript, kvScript);
    if (script == null) {
      log.warn("Script cache miss for key: {}", kvScript);
    }
    return script;
  }

  private LoadedScript loadScriptContentAndHash(KvScript script) throws IOException {
    final String fullPath = PathUtil.combinePaths(basePath, script.getClasspath());
    log.debug("Loading Lua script from classpath: {}", fullPath);
    final String content = IoUtil.getResourceAsString(fullPath);
    final String sha1 = CryptoUtil.calculateSha1(content);
    log.debug("Successfully loaded script '{}' (SHA-1: {})", script, sha1);
    return new LoadedScript(script, content, sha1);
  }
}
