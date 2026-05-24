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
import xyz.jwizard.buildconfig.JwServicePlugin
import xyz.jwizard.buildconfig.jwService

apply<JwServicePlugin>()

jwService {
    packageSuffix.set("api")
    mainClass.set("JwsApiMain")
}

dependencies {
    runtimeOnly(libs.postgresql)

    implementation(project(":jwl-codec"))
    implementation(project(":jwl-common"))
    implementation(project(":jwl-http"))
    implementation(project(":jwl-kv"))
    implementation(project(":jwl-sql"))

    testImplementation(testFixtures(project(":jwl-common")))
}
