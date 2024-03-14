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
import org.eclipse.kuksa.vssprocessor.plugin.generator.project.GradleProject.Companion.TEST_FOLDER_NAME_DEFAULT
import org.eclipse.kuksa.vssprocessor.plugin.generator.project.VssProcessorLibProject
import org.eclipse.kuksa.vssprocessor.plugin.generator.project.VssProcessorPluginProject
import org.eclipse.kuksa.vssprocessor.plugin.generator.project.dollar
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.pathString

// Note:
// - Debugging into functional gradle test cases do not work.
// - Sometimes test errors are obscured so try looking into the test report to see the actual one.
//
// ./gradlew :vss-processor-plugin:test -Dkotest.tags="Functional"
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

                `when`("the provideVssFiles task is executed without correct input") {
                    val result = gradleRunner
                        .withArguments(PROVIDE_VSS_FILES_TASK_NAME)
                        .buildAndFail()

                    then("it should throw an Exception") {
                        result.output shouldContain "Could not create task ':lib:provideVssFiles'"
                    }
                }
            }

            // The following tests are mainly testing the configuration cache
            // #1: A full clean of the build folder AND gradle cache -> SUCCESS
            // #2: A full clean of the build folder BUT gradle cache stays -> FROM_CACHE
            // #3: No clean -> UP_TO_DATE
            // #4: Gradle + plugin / task input changed -> SUCCESS
            // #5: Input file name changed -> SUCCESS
            and("a VSS compatible Android library project is added") {
                val vssFile2: File
                val vssProcessorProject = VssProcessorLibProject("lib").apply {
                    copyVssFiles(vssDir, VSS_TEST_FILE_NAME)
                    vssFile2 = copyVssFiles(vssDir2, VSS_TEST_FILE_MINIMAL_NAME)
                    generate()
                }
                val vssDir2 = vssProcessorProject.vssDir2
                val vssDir2Path = vssDir2.pathString

                pluginProject.add(vssProcessorProject)

                `when`("the provideVssFiles task is executed with build cache the #1 time") {
                    pluginProject.localCacheFolder.deleteRecursively()

                    val result = gradleRunner
                        .withArguments("clean", "--build-cache", PROVIDE_VSS_FILES_TASK_NAME)
                        .build()

                    println("ProvideVssFilesTask + Build Cache #1 output: ${result.output}")

                    then("it should build successfully") {
                        val outcome = result.task(":lib:$PROVIDE_VSS_FILES_TASK_NAME")?.outcome

                        outcome shouldBe TaskOutcome.SUCCESS
                    }
                }

                `when`("the provideVssFiles task is executed with build cache the #2 time") {
                    val result = gradleRunner
                        .withArguments("clean", "--build-cache", PROVIDE_VSS_FILES_TASK_NAME)
                        .build()

                    println("ProvideVssFilesTask + Build Cache #2 output: ${result.output}")

                    then("it should build from cache") {
                        val outcome = result.task(":lib:$PROVIDE_VSS_FILES_TASK_NAME")?.outcome

                        outcome shouldBe TaskOutcome.FROM_CACHE
                    }
                }

                `when`("the provideVssFiles task is executed with build cache the #3 time") {
                    val kspInputDir = vssProcessorProject.buildDir.resolve(KSP_INPUT_BUILD_DIRECTORY)
                    val result = gradleRunner
                        .withArguments("--build-cache", PROVIDE_VSS_FILES_TASK_NAME)
                        .build()

                    println("ProvideVssFilesTask + Build Cache #3 output: ${result.output}")

                    then("it should be up to date") {
                        val outcome = result.task(":lib:$PROVIDE_VSS_FILES_TASK_NAME")?.outcome

                        outcome shouldBe TaskOutcome.UP_TO_DATE
                    }

                    then("it should copy all vss files") {
                        val vssFile = kspInputDir.resolve(VSS_TEST_FILE_NAME)

                        vssFile.exists() shouldBe true
                    }
                }

                `when`("the input of the ProvideVssFilesTask changes") {
                    val projectVssDir2 = vssDir2Path.substringAfter(TEST_FOLDER_NAME_DEFAULT)
                    vssProcessorProject.generate(
                        """
                        vssProcessor {
                            searchPath = "${dollar}rootDir/$projectVssDir2"
                        }
                        """.trimIndent(),
                    )

                    val result = gradleRunner
                        .withArguments("--build-cache", PROVIDE_VSS_FILES_TASK_NAME)
                        .build()

                    println("ProvideVssFilesTask + Build Cache #4 output: ${result.output}")

                    then("it should build successfully") {
                        val outcome = result.task(":lib:$PROVIDE_VSS_FILES_TASK_NAME")?.outcome

                        outcome shouldBe TaskOutcome.SUCCESS
                    }
                }

                `when`("the name of the input of the ProvideVssFilesTask changes") {
                    vssFile2.renameTo(File("$vssDir2Path/vss_rel_4.0_test_renamed.yml"))
                    val result = gradleRunner
                        .withArguments("--build-cache", PROVIDE_VSS_FILES_TASK_NAME)
                        .build()

                    println("ProvideVssFilesTask + Build Cache #5 output: ${result.output}")

                    then("it should build successfully") {
                        val outcome = result.task(":lib:$PROVIDE_VSS_FILES_TASK_NAME")?.outcome

                        outcome shouldBe TaskOutcome.SUCCESS
                    }
                }
            }
        }
    }
}) {
    companion object {
        private const val VSS_TEST_FILE_NAME = "vss_rel_4.0_test.yaml"
        private const val PROVIDE_VSS_FILES_TASK_NAME = "provideVssFiles"
        private const val VSS_TEST_FILE_MINIMAL_NAME = "vss_rel_4.0_test_minimal.yaml"
        private const val GRADLE_VERSION_TEST = "8.6"
        private const val KSP_INPUT_BUILD_DIRECTORY = "kspInput"
    }
}
