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
import xyz.jwizard.buildconfig.JwProtobufPlugin
import xyz.jwizard.buildconfig.spotless.JwSpotlessProtobufPlugin

apply<JwProtobufPlugin>()
apply<JwSpotlessProtobufPlugin>()

dependencies {
  implementation(libs.jetty.client)
  implementation(libs.jetty.ws.client)

  implementation(project(":jwl-codec"))
  implementation(project(":jwl-common"))
  implementation(project(":jwl-net"))

  testImplementation(libs.awaitility)
  testImplementation(libs.protobuf.java)
  testImplementation(libs.wiremock)
  testImplementation(libs.ws.mock.server)

  testImplementation(project(":jwl-websocket"))
  testImplementation(testFixtures(project(":jwl-common")))
}
