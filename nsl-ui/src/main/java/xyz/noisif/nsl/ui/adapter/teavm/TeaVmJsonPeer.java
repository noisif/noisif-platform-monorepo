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
package xyz.noisif.nsl.ui.adapter.teavm;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import xyz.noisif.nsl.ui.spi.json.JsonPeer;

final class TeaVmJsonPeer implements JsonPeer {
  private final JSObject jsObj;

  TeaVmJsonPeer() {
    jsObj = createEmptyJsObject();
  }

  TeaVmJsonPeer(String rawJson) {
    jsObj = (rawJson == null || rawJson.isBlank()) ? createEmptyJsObject() : nativeParse(rawJson);
  }

  private TeaVmJsonPeer(JSObject jsObj) {
    this.jsObj = jsObj != null ? jsObj : createEmptyJsObject();
  }

  @Override
  public void putString(String key, String value) {
    setStringProp(jsObj, key, value);
  }

  @Override
  public void putNumber(String key, double value) {
    setNumberProp(jsObj, key, value);
  }

  @Override
  public void putBoolean(String key, boolean value) {
    setBooleanProp(jsObj, key, value);
  }

  @Override
  public void putObject(String key, JsonPeer nested) {
    if (nested instanceof TeaVmJsonPeer teaNested) {
      setObjectProp(jsObj, key, teaNested.jsObj);
    }
  }

  @Override
  public String getString(String key) {
    return getStringProp(jsObj, key);
  }

  @Override
  public Double getNumber(String key) {
    return has(key) ? getNumberProp(jsObj, key) : null;
  }

  @Override
  public Boolean getBoolean(String key) {
    return has(key) ? getBooleanProp(jsObj, key) : null;
  }

  @Override
  public JsonPeer getObject(String key) {
    JSObject nested = getObjectProp(jsObj, key);
    return nested != null ? new TeaVmJsonPeer(nested) : null;
  }

  @Override
  public boolean has(String key) {
    return hasProp(jsObj, key);
  }

  @Override
  public String toJsonString() {
    return nativeStringify(jsObj);
  }

  @JSBody(script = "return {};")
  private static native JSObject createEmptyJsObject();

  @JSBody(
      params = {"json"},
      script = "return JSON.parse(json);")
  private static native JSObject nativeParse(String json);

  @JSBody(
      params = {"obj"},
      script = "return JSON.stringify(obj);")
  private static native String nativeStringify(JSObject obj);

  @JSBody(
      params = {"obj", "key"},
      script =
          "return obj !== null && typeof obj === 'object' && (key in obj) && obj[key] !== null && obj[key] !== undefined;")
  private static native boolean hasProp(JSObject obj, String key);

  @JSBody(
      params = {"obj", "key", "val"},
      script = "obj[key] = val;")
  private static native void setStringProp(JSObject obj, String key, String val);

  @JSBody(
      params = {"obj", "key", "val"},
      script = "obj[key] = val;")
  private static native void setNumberProp(JSObject obj, String key, double val);

  @JSBody(
      params = {"obj", "key", "val"},
      script = "obj[key] = val;")
  private static native void setBooleanProp(JSObject obj, String key, boolean val);

  @JSBody(
      params = {"obj", "key", "val"},
      script = "obj[key] = val;")
  private static native void setObjectProp(JSObject obj, String key, JSObject val);

  @JSBody(
      params = {"obj", "key"},
      script = "var v = obj[key]; return (v !== null && v !== undefined) ? String(v) : null;")
  private static native String getStringProp(JSObject obj, String key);

  @JSBody(
      params = {"obj", "key"},
      script = "return Number(obj[key]);")
  private static native double getNumberProp(JSObject obj, String key);

  @JSBody(
      params = {"obj", "key"},
      script = "return Boolean(obj[key]);")
  private static native boolean getBooleanProp(JSObject obj, String key);

  @JSBody(
      params = {"obj", "key"},
      script = "var v = obj[key]; return (typeof v === 'object' && v !== null) ? v : null;")
  private static native JSObject getObjectProp(JSObject obj, String key);
}
