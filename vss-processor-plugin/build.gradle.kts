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

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    publish
    version
}

gradlePlugin {
    plugins {
        create("VssProcessorPlugin") {
            id = "org.eclipse.kuksa.vss-processor-plugin"
            implementationClass = "org.eclipse.kuksa.vssprocessor.plugin.VssProcessorPlugin"
        }
    }
}

group = "org.eclipse.kuksa"
version = rootProject.extra["projectVersion"].toString()

dependencies {
    implementation(kotlin("stdlib"))
}

configure<Publish_gradle.PublishPluginExtension> {
    mavenPublicationName = "release"
    componentName = "java"
    description = "Vehicle Signal Specification (VSS) Plugin of the KUKSA SDK"
}


