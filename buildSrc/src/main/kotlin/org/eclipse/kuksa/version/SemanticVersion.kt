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

package org.eclipse.kuksa.version

import java.io.File
import java.util.Locale

const val VERSION_FILE_DEFAULT_PATH_KEY = "versionFilePathKey"
const val VERSION_FILE_DEFAULT_NAME = "version.txt"

class SemanticVersion(versionFilePath: String) {
    val major: Int
    val minor: Int
    val patch: Int
    var suffix: String = ""

    val versionFile: File

    val versionName: String
        get() {
            var version = "$major.$minor.$patch"
            if (suffix.isNotEmpty()) {
                version += "-$suffix"
            }

            return version
        }

    val versionCode: Int
        get() {
            val decorator = "10"
            val paddedMajorVersion = String.format(Locale.ROOT, "%02d", major)
            val paddedMinorVersion = String.format(Locale.ROOT, "%02d", minor)
            val paddedPatchVersion = String.format(Locale.ROOT, "%02d", patch)

            return "$decorator$paddedMajorVersion$paddedMinorVersion$paddedPatchVersion".toInt()
        }

    init {
        versionFile = File(versionFilePath)

        val semanticVersion = versionFile.readText()
        val versions = semanticVersion.trim()
            .substringBefore("-") // Ignore suffixes like -SNAPSHOT
            .split(".")
        val suffix = semanticVersion.substringAfter("-", "")

        major = versions[0].toInt()
        minor = versions[1].toInt()
        patch = versions[2].toInt()
        this.suffix = suffix
    }
}
