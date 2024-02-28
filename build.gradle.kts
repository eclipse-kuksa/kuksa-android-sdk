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
