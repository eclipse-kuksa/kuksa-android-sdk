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

@file:Suppress("UnstableApiUsage")

import com.google.protobuf.gradle.id

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf")
    publish
}

group = "org.eclipse.kuksa"
version = rootProject.extra["projectVersion"].toString()

android {
    namespace = "org.eclipse.kuksa"
    compileSdk = 33

    defaultConfig {
        minSdk = 27

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
        kotlin {
            compilerOptions {
                // https://youtrack.jetbrains.com/issue/KT-48678/Coroutine-debugger-disable-was-optimised-out-compiler-feature
                // We don't want local variables to be optimized out while debugging into tests
                freeCompilerArgs.add("-Xdebug")
            }
        }
    }
    publishing {
        singleVariant("release") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}

dependencies {
    api(project(":vss-core")) // Models are exposed

    testImplementation(project(":test"))

    // needs to be api as long as we expose ProtoBuf specific objects
    api(libs.grpc.protobuf)

    implementation(kotlin("reflect"))

    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.stub)
    implementation(libs.tomcat.annotations)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotest)
    testImplementation(libs.mockk)
}

configure<Publish_gradle.PublishPluginExtension> {
    mavenPublicationName = "release"
    componentName = "release"
    description = "Android Connectivity Library for the KUKSA Databroker"
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        id("grpc") {
            artifact = libs.grpc.protoc.java.gen.get().toString()
        }
        generateProtoTasks {
            all().forEach {
                it.builtins {
                    create("java") {
                        option("lite")
                    }
                }
                it.plugins {
                    create("grpc") {
                        option("lite")
                    }
                }
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
    "setSnapshotVersion",
    "setReleaseVersion",
    "publishToMavenLocal",
    "publishAllPublicationsToOSSRHSnapshotRepository",
    "publishAllPublicationsToOSSRHReleaseRepository",
)
tasks
    .filter { dependentCompositeTasks.contains(it.name) }
    .forEach { task ->
        val compositeTask = gradle.includedBuilds.map { compositeBuild ->
            val compositeTaskPath = task.path.substringAfter(":kuksa-sdk")
            compositeBuild.task(compositeTaskPath)
        }

        task.dependsOn(compositeTask)
    }
