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
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.compileOnly
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.errorprone
import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.java
import org.gradle.kotlin.dsl.libs
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.spotless
import org.gradle.kotlin.dsl.testImplementation
import org.gradle.kotlin.dsl.testRuntimeOnly
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.model.IdeaModel
import xyz.jwizard.buildconfig.CompactTestOutputListener
import xyz.jwizard.buildconfig.buildLicense
import xyz.jwizard.buildconfig.getEnv
import xyz.jwizard.buildconfig.getPluginId
import xyz.jwizard.buildconfig.registerTestSummaryService

plugins {
    alias(libs.plugins.error.prone)
    alias(libs.plugins.java)
    alias(libs.plugins.idea)
    alias(libs.plugins.spotless)
}

allprojects {
    apply(plugin = getPluginId(rootProject.libs.plugins.idea))
    apply(plugin = getPluginId(rootProject.libs.plugins.spotless))

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

    spotless {
        val rawLicenseFile = rootProject.file("spotless/license-header.txt")
        java {
            target("src/**/*.java")
            targetExclude("build/generated/**/*.java")
            googleJavaFormat().aosp() // aosp with 4 space indentation
            // force enums to be as:
            // VALUE1,
            // VALUE2,
            // ;
            replaceRegex(
                "Force enum semicolon to new line with indent",
                "(?m)^([ \\t]*)(.*?)(,\\s*;)",
                "$1$2,\n$1;",
            )
            // related to eclipse style in .editorconfig, Google makes one gigantic ugly block :(
            importOrder("\\#", "com", "org", "xyz.jwizard", "", "jakarta", "java", "javax")
            licenseHeader(buildLicense(rawLicenseFile, "/*", " * ", " */"))
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            licenseHeader(
                buildLicense(rawLicenseFile, "/*", " * ", " */"),
                """(?m)^\s*[a-zA-Z@_]""",
            )
            ktlint()
            trimTrailingWhitespace()
            endWithNewline()
        }
        format("xml") {
            target("src/**/*.xml")
            eclipseWtp(EclipseWtpFormatterStep.XML)
                .configFile(rootProject.file("spotless/wtp-xml.prefs"))
            licenseHeader(buildLicense(rawLicenseFile, "<!--", "  ~ ", "  -->"), "^(<[^!?])")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}

subprojects {
    apply(plugin = getPluginId(rootProject.libs.plugins.java.library))
    apply(plugin = getPluginId(rootProject.libs.plugins.error.prone))

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    dependencies {
        implementation(rootProject.libs.slf4j.api)
        compileOnly(rootProject.libs.jspecify)
        errorprone(rootProject.libs.error.prone.core)
        compileOnly(rootProject.libs.error.prone.annotation)
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
            "-XX:+ExitOnOutOfMemoryError",
        )
        maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
        systemProperty(
            // avoids port conflicts during parallel execution by forcing testcontainers to use
            // the unix socket strategy
            "org.testcontainers.docker-client.strategy",
            "org.testcontainers.dockerclient.UnixSocketClientProviderStrategy",
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
            ),
        )
        options.errorprone {
            disableWarningsInGeneratedCode.set(true) // ignore autogenerated code to cut noise
            disable(
                // allow for using lists, arrays and lambdas inside enum fields
                "ImmutableEnumChecker",
                // for enable creating meta-annotations
                "InjectScopeAnnotationOnInterfaceOrAbstractClass",
            )
            error(
                "CheckReturnValue", // fail build if return value is ignored
                // no fire-and-forget async tasks, forces handling future results
                "FutureReturnValueIgnored",
                // block e.printstacktrace(), force proper logging via slf4j
                "CatchAndPrintStackTrace",
                "SystemOut", // block sysout to keep console clean and force logger usage
                "DefaultCharset", // force explicit charset to prevent prod encoding bugs
                // force nested classes to be static to prevent hidden memory leaks
                "ClassCanBeStatic",
                // block map.get() with wrong key types (prevents always-null bugs)
                "CollectionIncompatibleType",
                // strictly require @override to catch silently broken api contracts
                "MissingOverride",
            )
        }
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
            "-XX:+ExitOnOutOfMemoryError",
        )
    }
}
