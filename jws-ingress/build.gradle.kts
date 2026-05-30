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
import xyz.jwizard.buildconfig.JwServicePlugin
import xyz.jwizard.buildconfig.jwService

apply<JwServicePlugin>()
apply<JwProtobufPlugin>()

jwService {
  packageSuffix.set("ingress")
  mainClass.set("JwsIngressMain")
}

dependencies {
  implementation(libs.nv.websocket.client)
  implementation(libs.erlang.jinterface)

  implementation(project(":jwl-common"))
  implementation(project(":jwl-codec"))

  testImplementation(testFixtures(project(":jwl-common")))
}
