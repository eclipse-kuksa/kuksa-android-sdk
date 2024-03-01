/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.eclipse.kuksa.version.VERSION_FILE_DEFAULT_NAME
import org.eclipse.kuksa.version.VERSION_FILE_DEFAULT_PATH_KEY
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name
import kotlin.io.path.useLines
import kotlin.io.path.visitFileTree

val versionDefaultPath = "$rootDir/$VERSION_FILE_DEFAULT_NAME"
rootProject.ext[VERSION_FILE_DEFAULT_PATH_KEY] = versionDefaultPath

plugins {
    base
    detekt
    version
    kotlin("jvm")
    jacoco
}

subprojects {
    apply {
        plugin("ktlint")
        from("$rootDir/dash.gradle.kts")
    }
    afterEvaluate {
        tasks.check {
            finalizedBy("ktlintCheck")
        }
    }

    // see: https://kotest.io/docs/framework/tags.html#gradle
    tasks.withType<Test> {
        val systemPropertiesMap = HashMap<String, Any>()
        System.getProperties().forEach { key, value ->
            systemPropertiesMap[key.toString()] = value.toString()
        }
        systemProperties = systemPropertiesMap
    }
}

@OptIn(ExperimentalPathApi::class)
tasks.register("mergeDashFiles") {
    group = "oss"

    dependsOn(
        subprojects.map { subproject ->
            subproject.tasks.getByName("createDashFile")
        },
    )

    val buildDir = layout.buildDirectory.asFile.get()
    val buildDirPath = Path.of(buildDir.path)

    doLast {
        val ossDir = buildDirPath.resolve("oss").createDirectories()
        val ossAllDir = ossDir.resolve("all").createDirectories()
        val ossDependenciesFile = ossAllDir.resolve("all-dependencies.txt")
        ossDependenciesFile.deleteIfExists()
        ossDependenciesFile.createFile()

        val sortedLinesSet = sortedSetOf<String>()
        ossDir.visitFileTree {
            onVisitFile { file, _ ->
                if (file.name != "dependencies.txt") return@onVisitFile FileVisitResult.CONTINUE

                file.useLines {
                    sortedLinesSet.addAll(it)
                }

                FileVisitResult.CONTINUE
            }
        }

        val bufferedWriter = ossDependenciesFile.bufferedWriter()
        bufferedWriter.use { writer ->
            sortedLinesSet.forEach { line ->
                writer.write(line + System.lineSeparator())
            }
        }
    }
}

// region jacoco coverage report

subprojects {
    apply {
        plugin("jacoco")
    }

    if (plugins.hasPlugin("com.android.library")) {
        configure<LibraryExtension> {
            @Suppress("UnstableApiUsage")
            testOptions {
                buildTypes {
                    named("debug") {
                        enableUnitTestCoverage = true
                        enableAndroidTestCoverage = true
                    }
                }
            }
        }
    }

    if (plugins.hasPlugin("com.android.application")) {
        configure<BaseAppModuleExtension> {
            @Suppress("UnstableApiUsage")
            testOptions {
                buildTypes {
                    named("debug") {
                        enableUnitTestCoverage = true
                        enableAndroidTestCoverage = true
                    }
                }
            }
        }
    }
}

tasks.create("jacocoRootReport", JacocoReport::class.java) {
    group = "report"

    reports {
        html.required.set(true)
        xml.required.set(false)
        csv.required.set(false)
    }

    val excludes = listOf(
        "**/buildSrc/**",
        "**/app/**",
        "**/samples/**",
        "**/vssprocessor/plugin/**", // code coverage not supported for Gradle Plugins / Gradle TestKit tests
        "**/build/**/org/eclipse/kuksa/vss/**", // generated code
        "**/test*/**/*.*",
    )

    val sourcesKotlin = subprojects.map { it.layout.projectDirectory.dir("src/main/kotlin") }
    val sourcesJava = subprojects.map { it.layout.projectDirectory.dir("src/main/java") }

    val classes = fileTree(rootDir) {
        include(
            "**/build/classes/kotlin/**/*.class", // kotlin-jvm modules
            "**/build/tmp/kotlin-classes/debug/**/*.class", // android modules (application, libraries)
        )
        exclude(excludes)
    }.toSet()

    val executionPaths = fileTree(rootDir) {
        include("**/*.exec", "**/*.ec")
        exclude(excludes)
    }.toSet()

    additionalSourceDirs.setFrom(sourcesKotlin, sourcesJava)
    sourceDirectories.setFrom(sourcesKotlin, sourcesJava)
    classDirectories.setFrom(classes)
    executionData.setFrom(executionPaths)
}

// endregion jacoco
