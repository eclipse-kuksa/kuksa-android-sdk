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
    `maven-publish`
}

interface PublishPluginExtension {
    val mavenPublicationName: Property<String>
    val componentName: Property<String>
}

val extension = project.extensions.create<PublishPluginExtension>("publish")

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "GithubPackages"

                val repository = project.findProperty("gpr.repository") as String? ?: System.getenv("GPR_REPOSITORY")
                url = uri("https://maven.pkg.github.com/$repository")

                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USERNAME")
                    password = project.findProperty("gpr.token") as String? ?: System.getenv("GPR_TOKEN")
                }
            }
        }
        publications {
            register<MavenPublication>("${extension.mavenPublicationName.get()}") {
                from(components["${extension.componentName.get()}"])
            }
        }
    }
}
