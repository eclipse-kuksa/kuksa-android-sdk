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

import org.eclipse.kuksa.version.SemanticVersion
import org.eclipse.kuksa.version.VERSION_FILE_DEFAULT_NAME

private val versionPath = "$rootDir/$VERSION_FILE_DEFAULT_NAME"
private val semanticVersion = SemanticVersion(versionPath)

/**
 * Writes the given [suffix] into the given [inputFileProperty] while keeping the semantic version intact.
 * E.g. 1.2.3 -> 1.2.3-SNAPSHOT (suffix = SNAPSHOT). Leave the suffix empty to restore the initial version.
 */
abstract class SetVersionSuffixTask : DefaultTask() {
    @get:Input
    abstract val suffix: Property<String>

    @get:Incremental
    @get:InputFile
    abstract val inputFileProperty: RegularFileProperty

    @TaskAction
    fun addVersionSuffix() {
        val inputFile = inputFileProperty.asFile.get()

        val newSemanticVersion = SemanticVersion(inputFile.path)
        newSemanticVersion.suffix = suffix.get()

        println("Applying version suffix: ${suffix.get()}")

        inputFile.writeText(newSemanticVersion.versionName)
    }
}

// Do not chain this command because it writes into a file which needs to be re-read inside the next gradle command
tasks.register<SetVersionSuffixTask>("setSnapshotVersion") {
    group = "version"

    inputFileProperty = semanticVersion.versionFile
    suffix = "SNAPSHOT"
}

// Do not chain this command because it writes into a file which needs to be re-read inside the next gradle command
tasks.register<SetVersionSuffixTask>("setReleaseVersion") {
    group = "version"

    inputFileProperty = semanticVersion.versionFile
    suffix = ""
}

tasks.register("printVersion") {
    group = "version"

    val versionFilePath = versionPath
    doLast { // Prints the correct version if chained with SetVersionSuffix tasks
        val currentSemanticVersion = SemanticVersion(versionFilePath)
        println("Current version: ${currentSemanticVersion.versionName}")
    }

    mustRunAfter("setReleaseVersion", "setSnapshotVersion")
}
