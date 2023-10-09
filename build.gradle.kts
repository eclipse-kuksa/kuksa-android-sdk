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

import org.jetbrains.kotlin.incremental.createDirectory

plugins {
    base
    detekt
    version
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

tasks.register("mergeDashFiles") {
    group = "oss"

    dependsOn(
        subprojects.map { subproject ->
            subproject.tasks.getByName("createDashFile")
        },
    )

    doLast {
        val sortedLinesSet = sortedSetOf<String>()
        files("build/oss").asFileTree.forEach { file ->
            if (file.name != "dependencies.txt") return@forEach

            file.useLines {
                sortedLinesSet.addAll(it)
            }
        }

        val folder = File("$rootDir/build/oss/all")
        folder.createDirectory()

        val file = File("$folder/all-dependencies.txt")
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()

        val bufferedWriter = file.bufferedWriter()
        bufferedWriter.use { writer ->
            sortedLinesSet.forEach { line ->
                writer.write(line + System.lineSeparator())
            }
        }
    }
}
