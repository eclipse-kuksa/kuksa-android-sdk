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
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

class VssProcessorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Configuration done by the custom task
    }
}

/**
 * This task takes a list of files and copies them into an input folder for the KSP VSS Processor. This is necessary
 * because the Symbol Processor does not have access to the android assets folder.
 */
abstract class ProvideVssDefinitionTask : DefaultTask() {

    @get:InputFiles
    abstract val vssDefinitionFile: ListProperty<RegularFile>

    @TaskAction
    fun provideFile() {
        val buildDirPath = project.buildDir.absolutePath
        vssDefinitionFile.get().forEach { file ->
            val vssDefinitionInputFile = file.asFile
            val vssDefinitionBuildFile = File(
                "$buildDirPath$fileSeparator" +
                    "$KSP_INPUT_BUILD_DIRECTORY$fileSeparator" +
                    vssDefinitionInputFile.name,
            )

            println(
                "Found VSS definition input file: ${vssDefinitionInputFile.name}, copying to: $vssDefinitionBuildFile",
            )

            vssDefinitionInputFile.copyTo(vssDefinitionBuildFile, true)
        }
    }

    companion object {
        private const val KSP_INPUT_BUILD_DIRECTORY = "kspInput"

        private val fileSeparator = File.separator
    }
}
