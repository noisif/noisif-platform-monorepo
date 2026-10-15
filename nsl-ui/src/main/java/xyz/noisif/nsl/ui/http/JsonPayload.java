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
package xyz.noisif.nsl.ui.http;

import xyz.noisif.nsl.ui.core.UiContext;
import xyz.noisif.nsl.ui.spi.json.JsonPeer;

public final class JsonPayload {
  private final JsonPeer peer;

  private JsonPayload(JsonPeer peer) {
    this.peer = peer;
  }

  public static JsonPayload create() {
    return new JsonPayload(UiContext.toolkit().createJsonObject());
  }

  public static JsonPayload parse(String rawJson) {
    return new JsonPayload(UiContext.toolkit().parseJson(rawJson));
  }

  public JsonPayload put(String key, String value) {
    peer.putString(key, value);
    return this;
  }

  public JsonPayload put(String key, Number value) {
    if (value != null) {
      peer.putNumber(key, value.doubleValue());
    }
    return this;
  }

  public JsonPayload put(String key, boolean value) {
    peer.putBoolean(key, value);
    return this;
  }

  public JsonPayload put(String key, JsonPayload nestedObject) {
    if (nestedObject != null) {
      peer.putObject(key, nestedObject.peer);
    }
    return this;
  }

  public boolean has(String key) {
    return peer.has(key);
  }

  public String getString(String key) {
    return peer.getString(key);
  }

  public String getStringOrDefault(String key, String defaultValue) {
    String val = peer.getString(key);
    return val != null ? val : defaultValue;
  }

  public int getInt(String key) {
    Double val = peer.getNumber(key);
    return val != null ? val.intValue() : 0;
  }

  public long getLong(String key) {
    Double val = peer.getNumber(key);
    return val != null ? val.longValue() : 0L;
  }

  public boolean getBoolean(String key) {
    Boolean val = peer.getBoolean(key);
    return Boolean.TRUE.equals(val);
  }

  public JsonPayload getObject(String key) {
    JsonPeer nested = peer.getObject(key);
    return nested != null ? new JsonPayload(nested) : null;
  }

  public String toJson() {
    return peer.toJsonString();
  }
}
