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
package xyz.noisif.nsl.netclient;

import xyz.noisif.nsl.codec.serialization.json.JacksonSerializer;
import xyz.noisif.nsl.codec.serialization.json.JsonSerializer;
import xyz.noisif.nsl.common.bootstrap.ForbiddenInstantiationException;

public class TestConstants {
  public static final String SECRET_TOKEN = "super-secret-noisif-key";
  public static final String SERVICE_NAME = "internal-microservice";
  public static final JsonSerializer JSON_SERIALIZER =
      JacksonSerializer.createLenientForMessaging();
  public static final String DATA_TYPE_QUERY_PARAM_NAME = "frame";

  private TestConstants() {
    throw new ForbiddenInstantiationException(TestConstants.class);
  }
}
