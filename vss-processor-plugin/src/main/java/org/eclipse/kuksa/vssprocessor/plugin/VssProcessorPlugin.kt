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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

class VssProcessorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
    }
}

abstract class ProvideVssDefinitionTask : DefaultTask() {

    @get:InputFile
    abstract val vssDefinitionFile: RegularFileProperty

    @TaskAction
    fun provideFile() {
        val vssDefinitionInputFile = vssDefinitionFile.get().asFile
        val buildDirPath = project.buildDir.absolutePath
        val vssDefinitionBuildFile = File("$buildDirPath/$KSP_INPUT_BUILD_DIRECTORY/${vssDefinitionInputFile.name}")

        println("Found VSS definition input file: ${vssDefinitionInputFile.name}, copying to: $vssDefinitionBuildFile")

        vssDefinitionInputFile.copyTo(vssDefinitionBuildFile, true)
    }

    companion object {
        private const val KSP_INPUT_BUILD_DIRECTORY = "kspInput/"
    }
}
