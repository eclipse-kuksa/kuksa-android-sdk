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

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

open class VssProcessorPluginExtension
@Inject
internal constructor(objectFactory: ObjectFactory) {
    /**
     * The default search path is the $rootProject/vss folder. The defined folder will be crawled for all compatible
     * extension types by this plugin.
     */
    val searchPath: Property<String> = objectFactory.property(String::class.java).convention("")
}

private val fileSeparator = File.separator

/**
 * This Plugin searches for compatible VSS files and copies them into an input folder for the
 * KSP VSS Processor. This is necessary because the Symbol Processor does not have access to the android assets folder.
 */
class VssProcessorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<VssProcessorPluginExtension>(EXTENSION_NAME)

        // The extension variables are only available after the project has been evaluated
        project.afterEvaluate {
            val buildDir = layout.buildDirectory.asFile.get()
            val buildDirPath = buildDir.absolutePath
            val vssDir = "${rootDir}${fileSeparator}$VSS_FOLDER_NAME"

            val provideVssFilesTask =
                project.tasks.register<ProvideVssFilesTask>(PROVIDE_VSS_FILES_TASK_NAME) {
                    val searchPath = extension.searchPath.get().ifEmpty { vssDir }
                    val vssFilePath = StringBuilder(buildDirPath)
                        .append(fileSeparator)
                        .append(KSP_INPUT_BUILD_DIRECTORY)
                        .append(fileSeparator)
                        .toString()
                    val vssBuildFile = File(vssFilePath)

                    logger.info("Searching directory $searchPath for VSS definitions")

                    val searchDir = file(searchPath)
                    if (!searchDir.exists()) {
                        throw FileNotFoundException(
                            "Directory '$searchPath' for VSS files not found! Please create the folder relative to " +
                                "your project directory: ${searchDir.path}.",
                        )
                    }

                    inputDir.set(searchDir)
                    outputDir.set(vssBuildFile)
                }

            tasks.getByName("preBuild").dependsOn(
                provideVssFilesTask.get(),
            )
        }
    }

    companion object {
        private const val KSP_INPUT_BUILD_DIRECTORY = "kspInput"
        private const val EXTENSION_NAME = "vssProcessor"
        private const val PROVIDE_VSS_FILES_TASK_NAME = "provideVssFiles"
        private const val VSS_FOLDER_NAME = "vss"
    }
}

/**
 * This task takes an input directory [inputDir] which should contain all available VSS files and an
 * output directory [outputDir] where all files are copied to so the VSSProcessor can work with them.
 */
@CacheableTask
private abstract class ProvideVssFilesTask : DefaultTask() {
    @get:Incremental
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun provideFile(inputChanges: InputChanges) {
        inputChanges.getFileChanges(inputDir).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach

            val file = change.file
            val extension = file.extension
            if (!validVssExtension.contains(extension)) {
                logger.warn("Found incompatible VSS file: ${file.name} - Consider removing it")
                return@forEach
            }

            val targetFile = outputDir.file(change.normalizedPath).get().asFile
            logger.info("Found VSS file changes for: ${targetFile.name}, change: ${change.changeType}")

            when (change.changeType) {
                ChangeType.ADDED,
                ChangeType.MODIFIED,
                -> file.copyTo(targetFile, true)

                ChangeType.REMOVED -> targetFile.delete()
                else -> logger.warn("Could not determine file change type: ${change.changeType}")
            }
        }
    }

    companion object {
        private val validVssExtension = setOf("yml", "yaml")
    }
}
