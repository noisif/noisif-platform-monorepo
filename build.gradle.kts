/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.gradle.plugins.ide.idea.model.IdeaModel
import xyz.jwizard.buildconfig.CompactTestOutputListener
import xyz.jwizard.buildconfig.getEnv
import xyz.jwizard.buildconfig.getPluginId
import xyz.jwizard.buildconfig.registerTestSummaryService

plugins {
    alias(libs.plugins.java)
    alias(libs.plugins.idea)
}

allprojects {
    apply(plugin = getPluginId(rootProject.libs.plugins.idea))

    group = "xyz.jwizard"
    version = getEnv("VERSION", "0.0.0")

    repositories {
        mavenCentral()
        maven {
            // for gradle tooling api
            url = uri("https://repo.gradle.org/gradle/libs-releases")
        }
    }

    configure<IdeaModel> {
        module {
            excludeDirs.add(file(".bin"))
            excludeDirs.add(file(".kotlin"))
        }
    }
}

subprojects {
    apply(plugin = getPluginId(rootProject.libs.plugins.java.library))

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    dependencies {
        implementation(rootProject.libs.slf4j.api)
        compileOnly(rootProject.libs.jspecify)
        testImplementation(rootProject.libs.assertj.core)
        testImplementation(rootProject.libs.junit.jupiter)
        testImplementation(rootProject.libs.mockito.core)
        testImplementation(rootProject.libs.mockito.jupiter)
        testRuntimeOnly(rootProject.libs.logback.classic)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = false
        }
        val summaryService = registerTestSummaryService()
        usesService(summaryService)
        addTestListener(CompactTestOutputListener(summaryService.get()))
        // suppress JDK 21+ warnings regarding dynamic agent loading (used by mockito)
        // -Xshare:off: disables class data sharing
        jvmArgs(
            "-XX:+EnableDynamicAgentLoading",
            "-Xshare:off",
            // GC
            "-XX:+UseZGC",
            "-XX:+ZGenerational", // high-performance, low-latency gc for java 21+
            // memory
            "-Xms2G",
            "-Xmx2G",
            "-XX:MaxMetaspaceSize=512m", // 512 for protobuf one-class-per-file convention
            "-XX:+AlwaysPreTouch", // zero latency spikes on memory allocation by pre-touching pages
            "-XX:+UseStringDeduplication", // saves ram by removing duplicate strings from heap
            // others
            "-Dfile.encoding=UTF-8",
            "-XX:+ExitOnOutOfMemoryError"
        )
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(
            listOf(
                "-Werror", // change warnings to errors
                "-Xdoclint:all,-missing", // for Javadoc
                "-Xlint:cast",
                "-Xlint:deprecation",
                "-Xlint:fallthrough", // for classic switch statements
                "-Xlint:rawtypes",
                "-Xlint:serial", // for serialization
                "-Xlint:unchecked", // show all unchecked and insecure java casts
            )
        )
    }

    tasks.withType<JavaExec> {
        jvmArgs(
            "-Dlogback.configurationFile=logback-dev.xml",
            // GC
            "-XX:+UseZGC",
            "-XX:+ZGenerational",
            // memory
            "-Xms4G",
            "-Xmx4G",
            "-XX:MaxMetaspaceSize=512m",
            "-XX:+AlwaysPreTouch",
            "-XX:+UseStringDeduplication",
            "-Dfile.encoding=UTF-8",
            "-XX:+ExitOnOutOfMemoryError"
        )
    }
}
