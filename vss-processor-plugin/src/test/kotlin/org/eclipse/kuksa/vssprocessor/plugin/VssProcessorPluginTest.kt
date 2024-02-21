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

package org.eclipse.kuksa.vssprocessor.plugin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.eclipse.kuksa.vssprocessor.plugin.generator.project.AndroidLibProject
import org.eclipse.kuksa.vssprocessor.plugin.generator.project.VssProcessorLibProject
import org.eclipse.kuksa.vssprocessor.plugin.generator.project.VssProcessorLibProject.Companion.VSS_TEST_FILE
import org.eclipse.kuksa.vssprocessor.plugin.generator.project.VssProcessorPluginProject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists

@OptIn(ExperimentalPathApi::class)
class VssProcessorPluginTest : BehaviorSpec({
    tags(Functional)

    given("A VSSProcessorPlugin project") {
        val pluginProject = VssProcessorPluginProject()
        val rootProjectDir = pluginProject.rootProjectDir.toFile()
        val gradleRunner = GradleRunner.create()
            .forwardOutput()
            .withGradleVersion(GRADLE_VERSION_TEST)
            .withPluginClasspath()
            .withProjectDir(rootProjectDir)

        afterSpec {
            pluginProject.close()
        }

        `when`("the plugin is applied") {
            pluginProject.generate()

            val pluginResult = gradleRunner.build()

            then("it should build successfully") {
                pluginResult.output shouldContain "BUILD SUCCESSFUL"
            }

            and("an Android library project is added") {
                val androidLibProject = AndroidLibProject("lib")
                androidLibProject.generate()

                pluginProject.add(androidLibProject)

                afterContainer {
                    pluginProject.refresh() // So the plugin project does not have 2 :lib includes
                }

                `when`("the ProvideVssDefinitionTask is executed without correct input") {
                    val result = gradleRunner
                        .withArguments("provideVssDefinition")
                        .buildAndFail()

                    then("it should throw an Exception") {
                        result.output shouldContain "Could not create task ':lib:provideVssDefinition'"
                    }
                }
            }

            and("a VSS compatible Android library project is added") {
                val vssProcessorProject = VssProcessorLibProject("lib")
                vssProcessorProject.generate()

                pluginProject.add(vssProcessorProject)

                `when`("the ProvideVssDefinitionTask is executed with build cache the #1 time") {
                    pluginProject.localCacheFolder.deleteRecursively()

                    val result = gradleRunner
                        .withArguments("clean", "--build-cache", "provideVssDefinition")
                        .build()

                    println("ProvideVssDefinitionTask + Build Cache #1 output: ${result.output}")

                    then("it should build successfully") {
                        val outcome = result.task(":lib:provideVssDefinition")?.outcome

                        outcome shouldBe TaskOutcome.SUCCESS
                    }
                }

                `when`("the ProvideVssDefinitionTask is executed with build cache the #2 time") {
                    val result = gradleRunner
                        .withArguments("clean", "--build-cache", "provideVssDefinition")
                        .build()

                    println("ProvideVssDefinitionTask + Build Cache #2 output: ${result.output}")

                    then("it should build from cache") {
                        val outcome = result.task(":lib:provideVssDefinition")?.outcome

                        outcome shouldBe TaskOutcome.FROM_CACHE
                    }
                }

                `when`("the ProvideVssDefinitionTask is executed with build cache the #3 time") {
                    val kspInputDir = vssProcessorProject.buildDir.resolve(KSP_INPUT_BUILD_DIRECTORY)
                    val result = gradleRunner
                        .withArguments("--build-cache", "provideVssDefinition")
                        .build()

                    println("ProvideVssDefinitionTask + Build Cache #3 output: ${result.output}")

                    then("it should be up to date") {
                        val outcome = result.task(":lib:provideVssDefinition")?.outcome

                        outcome shouldBe TaskOutcome.UP_TO_DATE
                    }

                    then("it should copy all vss files") {
                        val vssFile = kspInputDir.resolve(VSS_TEST_FILE)

                        vssFile.exists() shouldBe true
                    }
                }
            }
        }
    }
}) {
    companion object {
        private const val GRADLE_VERSION_TEST = "8.6"
        private const val KSP_INPUT_BUILD_DIRECTORY = "kspInput"
    }
}
