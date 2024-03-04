import org.eclipse.kuksa.version.SemanticVersion
import org.eclipse.kuksa.version.VERSION_FILE_DEFAULT_PATH_KEY
import org.jetbrains.dokka.gradle.DokkaTask

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

plugins {
    kotlin("jvm")
    publish
    alias(libs.plugins.dokka)
}

val versionPath = rootProject.ext[VERSION_FILE_DEFAULT_PATH_KEY] as String
val semanticVersion = SemanticVersion(versionPath)
version = semanticVersion.versionName
group = "org.eclipse.kuksa"

dependencies {
    implementation(project(":vss-core"))

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation(libs.gson)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.symbol.processing.api)

    testImplementation(libs.kotest)
    testImplementation(libs.mockk)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    kotlin {
        compilerOptions {
            // https://youtrack.jetbrains.com/issue/KT-48678/Coroutine-debugger-disable-was-optimised-out-compiler-feature
            // We don't want local variables to be optimized out while debugging into tests
            freeCompilerArgs.add("-Xdebug")
        }
    }
}

publish {
    mavenPublicationName = "release"
    componentName = "java"
    description = "Vehicle Signal Specification (VSS) Code Generator for the KUKSA SDK"
}

tasks.register("javadocJar", Jar::class) {
    dependsOn("dokkaHtml")

    val buildDir = layout.buildDirectory.get()
    from("$buildDir/dokka/html")
    archiveClassifier.set("javadoc")
}

tasks.withType<DokkaTask>().configureEach {
    notCompatibleWithConfigurationCache("https://github.com/Kotlin/dokka/issues/2231")
}

java {
    withJavadocJar() // needs to be called after tasks.register("javadocJar")
    withSourcesJar()
}

// Tasks for included composite builds need to be called separately. For convenience sake we depend on the most used
// tasks. Every task execution of this project will then be forwarded to the included build project. Since this module
// is hard coupled to the
//
// We have to manually define the task names because the task() method of the included build throws an error for any
// unknown task.
//
// WARNING: Do not depend on the task "clean" here: https://github.com/gradle/gradle/issues/23585
val dependentCompositeTasks = setOf(
    "publishToMavenLocal",
    "publishAllPublicationsToOSSRHReleaseRepository",
    "test",
)
val dependentCompositeBuilds = setOf("vss-processor-plugin")

gradle.projectsEvaluated {
    val subProjectTasks = tasks + subprojects.flatMap { it.tasks }

    println("Linking Composite Tasks:")

    subProjectTasks
        .filter { dependentCompositeTasks.contains(it.name) }
        .forEach { task ->
            val compositeTask = gradle.includedBuilds
                .filter { dependentCompositeBuilds.contains(it.name) }
                .map { compositeBuild ->
                    println("- ${task.project.name}:${task.name} -> ${compositeBuild.name}:${task.name}")

                    compositeBuild.task(":${task.name}")
                }

            task.dependsOn(compositeTask)
        }
}
