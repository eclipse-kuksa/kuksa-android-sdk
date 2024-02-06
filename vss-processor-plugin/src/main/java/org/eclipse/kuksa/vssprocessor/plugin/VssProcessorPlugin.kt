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
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import java.io.File
import javax.inject.Inject

open class VssProcessorPluginExtension
@Inject
internal constructor(objectFactory: ObjectFactory) {
    /**
     * The default search path is the main assets folder. The defined folder will be crawled for all compatible
     * extension types by this plugin.
     */
    val searchPath: Property<String> = objectFactory.property(String::class.java).convention("")

    /**
     * If no file name is provided then all compatible files inside the [searchPath] will be copied. Otherwise only
     * the defined file will be used if available.
     */
    val fileName: Property<String> = objectFactory.property(String::class.java).convention("")
}

private val fileSeparator = File.separator

/**
 * This Plugin searches for compatible specification files and copies them into an input folder for the
 * KSP VSS Processor. This is necessary because the Symbol Processor does not have access to the android assets folder.
 * The default search path is the main assets folder.
 */
class VssProcessorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<VssProcessorPluginExtension>(EXTENSION_NAME)

        val mainAssetsDirectory = "${project.projectDir}${fileSeparator}$mainAssetsDirectory"
        val searchDirectory = extension.searchPath.get().ifEmpty { mainAssetsDirectory }

        val compatibleFiles = findFiles(
            directory = searchDirectory,
            fileName = extension.fileName.get(),
            validExtensions = compatibleExtensions,
        )

        val provideVssDefinitionTask = project.tasks.register<ProvideVssDefinitionTask>(PROVIDE_VSS_DEFINITION_TASK) {
            compatibleFiles.forEach { definitionFile ->
                val regularFile = RegularFile { definitionFile }
                vssDefinitionFiles.add(regularFile)
            }
        }

        project.tasks.getByName("preBuild").finalizedBy(
            provideVssDefinitionTask.get(),
        )
    }

    @Suppress("SameParameterValue")
    private fun findFiles(
        directory: String,
        fileName: String = "",
        validExtensions: Collection<String>,
    ): Sequence<File> {
        val mainAssetsFolder = File(directory)

        return mainAssetsFolder
            .walk()
            .filter { validExtensions.contains(it.extension) }
            .filter { file ->
                if (fileName.isEmpty()) return@filter true

                file.name == fileName
            }
    }

    companion object {
        private const val EXTENSION_NAME = "vssProcessor"
        private const val PROVIDE_VSS_DEFINITION_TASK = "ProvideVssDefinition"

        private val compatibleExtensions = setOf("yaml")
        private val mainAssetsDirectory = "src$fileSeparator" + "main$fileSeparator" + "assets"
    }
}

/**
 * This task takes a collection of files
 */
abstract class ProvideVssDefinitionTask : DefaultTask() {
    @get:InputFiles
    abstract val vssDefinitionFiles: ListProperty<RegularFile>

    @TaskAction
    fun provideFile() {
        val buildDirPath = project.buildDir.absolutePath
        vssDefinitionFiles.get().forEach { file ->
            val vssDefinitionInputFile = file.asFile
            println("Searching for VSS file: ${vssDefinitionInputFile.absolutePath}")
            val vssDefinitionBuildFile = File(
                "$buildDirPath$fileSeparator" +
                    "$KSP_INPUT_BUILD_DIRECTORY$fileSeparator" +
                    vssDefinitionInputFile.name,
            )

            println("Found VSS input file: ${vssDefinitionInputFile.name}, copying to: $vssDefinitionBuildFile")

            vssDefinitionInputFile.copyTo(vssDefinitionBuildFile, true)
        }
    }

    companion object {
        private const val KSP_INPUT_BUILD_DIRECTORY = "kspInput"
    }
}
