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
 */

package org.eclipse.kuksa.vssprocessor.plugin.generator.project

import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class VssProcessorPluginProject : RootGradleProject("VssProcessorPlugin") {
    val localCacheFolder = projectDir.resolve("local-cache").createDirectories()

    override fun generate() {
        settingsFile.writeText(
            """
                pluginManagement {
                    includeBuild("../../../vss-processor-plugin")

                    repositories {
                        gradlePluginPortal()
                        google()
                        mavenCentral()
                    }
                }

                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                    repositories {
                        mavenLocal()
                        google()
                        mavenCentral()
                    }
                    versionCatalogs {
                        create("libs") {
                            from(files("../../../gradle/libs.versions.toml"))
                        }
                    }
                }

                buildCache {
                    local {
                        directory = "${localCacheFolder.toFile().toURI()}"
                    }
                }

                rootProject.name = "${this::class.simpleName}Test"
            """.trimIndent(),
        )

        rootBuildFile.writeText(
            """
                buildscript {
                    repositories {
                        google()
                        mavenCentral()
                    }

                    dependencies {
                        classpath("com.android.tools.build:gradle:$AGP_VERSION")
                        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION")
                    }
                }
            """.trimIndent(),
        )
    }

    companion object {
        private const val AGP_VERSION = "8.2.2"
        private const val KOTLIN_VERSION = "1.9.22"
    }
}
