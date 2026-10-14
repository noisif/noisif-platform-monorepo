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
import xyz.noisif.buildconfig.apply
import xyz.noisif.buildconfig.spa.teavm.NsSpaTeaVmFrontendPlugin
import xyz.noisif.buildconfig.spa.teavm.nsSpaTeaVm
import xyz.noisif.buildconfig.spa.teavm.spaFrontend

pluginManager.apply<NsSpaTeaVmFrontendPlugin>()

nsSpaTeaVm {
  title.set("NSS Registry Panel")
  packageSuffix.set("registry.ui")
  mainClass.set("NssRegistryUiMain")
  generateHash.set(false)
  minify.set(true)
  devServer {
    port.set(9233)
    proxy("/api") {
      url.set("http://localhost:9231")
      stripPrefix.set(false)
    }
  }
}

dependencies {
  spaFrontend(project(":nsl-ui"))

  testImplementation(testFixtures(project(":nsl-common")))
}
