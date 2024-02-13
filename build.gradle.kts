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
import org.jetbrains.kotlin.incremental.createDirectory

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

tasks.register("mergeDashFiles") {
    group = "oss"

    dependsOn(
        subprojects.map { subproject ->
            subproject.tasks.getByName("createDashFile")
        },
    )

    val ossFolder = File("$rootDir/build/oss/all")
    ossFolder.createDirectory()

    val ossDependenciesFile = File("$ossFolder/all-dependencies.txt")
    if (ossDependenciesFile.exists()) {
        ossDependenciesFile.delete()
    }
    ossDependenciesFile.createNewFile()

    val ossFiles = files("build/oss")
    doLast {
        val sortedLinesSet = sortedSetOf<String>()
        ossFiles.asFileTree.forEach { file ->
            if (file.name != "dependencies.txt") return@forEach

            file.useLines {
                sortedLinesSet.addAll(it)
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

// Tasks for included composite builds need to be called separately. For convenience sake we depend on the most used
// tasks. Every task execution of the main project will then be forwarded to the included build project.
//
// We have to manually define the task names because the task() method of the included build throws an error for any
// unknown task.
val dependentCompositeTasks = setOf(
    "publishToMavenLocal",
    "publishAllPublicationsToOSSRHReleaseRepository",
)

gradle.projectsEvaluated {
    val subProjectTasks = tasks + subprojects.flatMap { it.tasks }

    subProjectTasks
        .filter { dependentCompositeTasks.contains(it.name) }
        .forEach { task ->
            val compositeTask = gradle.includedBuilds.map { compositeBuild ->
                val compositeTaskPath = task.path.substringAfterLast(":")
                println("Linking composite task - ${compositeBuild.name} <-> ${task.project}:${task.name}")

                compositeBuild.task(":$compositeTaskPath")
            }

            task.dependsOn(compositeTask)
        }
}
