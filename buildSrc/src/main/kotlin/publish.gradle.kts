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
    signing
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
                name = "OSSRHRelease"

                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("ORG_OSSRH_USERNAME")
                    password = System.getenv("ORG_OSSRH_PASSWORD")
                }
            }
            maven {
                name = "OSSRHSnapshot"

                url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                credentials {
                    username = System.getenv("ORG_OSSRH_USERNAME")
                    password = System.getenv("ORG_OSSRH_PASSWORD")
                }
            }
        }
        publications {
            register<MavenPublication>("${extension.mavenPublicationName.get()}") {
                from(components["${extension.componentName.get()}"])
            }
        }
    }

    signing {
        val signingKey = System.getenv("ORG_GPG_PRIVATE_KEY")
        val signingPassword = System.getenv("ORG_GPG_PASSPHRASE")
        useInMemoryPgpKeys(signingKey, signingPassword)

        useGpgCmd()
        sign(publishing.publications)
    }
}

gradle.taskGraph.whenReady {
    tasks.withType(Sign::class) {
        val publishToMavenLocalTask = allTasks.find { it.name.contains("publishToMavenLocal") }
        val isPublishingToMavenLocal = publishToMavenLocalTask != null

        if (isPublishingToMavenLocal) {
            println(":${project.name}:$name - Signing is disabled (publishToMavenLocal)")
        }

        onlyIf { !isPublishingToMavenLocal } // disable signing when publishing to MavenLocal
    }
}
