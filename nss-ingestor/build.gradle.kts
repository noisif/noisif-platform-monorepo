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
import xyz.noisif.buildconfig.polyglot.NsPolyglotJsPlugin
import xyz.noisif.buildconfig.polyglot.nsPolyglotJs
import xyz.noisif.buildconfig.service.NsServicePlugin
import xyz.noisif.buildconfig.service.nsService

apply<NsServicePlugin>()
apply<NsPolyglotJsPlugin>()

nsService {
  packageSuffix.set("ingestor")
  mainClass.set("NssIngestorMain")
}

nsPolyglotJs {
  entryPoints.put("yarn-parser.bundle", "node_modules/@yarnpkg/parsers/lib/index.js")
  npmDependencies.add("@yarnpkg/parsers")
}

dependencies {
  implementation(libs.graalvm.polyglot)
  implementation(libs.graalvm.polyglot.js)
  implementation(libs.gradle.tooling.api)

  implementation(project(":nsl-codec"))
  implementation(project(":nsl-common"))
  implementation(project(":nsl-graph"))
  implementation(project(":nsl-net"))
  implementation(project(":nsl-websocket"))

  testImplementation(testFixtures(project(":nsl-common")))
}
