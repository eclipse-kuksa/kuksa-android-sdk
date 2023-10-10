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

package org.eclipse.kuksa.util

import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Locale
import java.util.Properties

class VersionProperties(private val filePath: String) {
    private val properties: Properties = Properties()

    var major: Int
        get() = properties.getProperty(KEY_MAJOR).toInt()
        set(value) {
            properties.put(KEY_MAJOR, value.toString())
        }

    var minor: Int
        get() = properties.getProperty(KEY_MINOR).toInt()
        set(value) {
            properties.put(KEY_MINOR, value.toString())
        }

    var patch: Int
        get() = properties.getProperty(KEY_PATCH).toInt()
        set(value) {
            properties.put(KEY_PATCH, value.toString())
        }

    var suffix: String
        get() = properties.getProperty(KEY_SUFFIX)
        set(value) {
            properties.put(KEY_SUFFIX, value)
        }

    val version: String
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

    fun load() {
        try {
            val file = File(filePath)
            val inputStream = file.inputStream()

            properties.load(inputStream)
        } catch (e: IOException) {
            System.err.println("Could not load file $filePath: ${e.message}")
        }
    }

    fun store() {
        try {
            val file = File(filePath)
            val fileWriter = FileWriter(file)

            properties.store(fileWriter, "Generated by 'store' in VersionProperties.kt")
        } catch (e: IOException) {
            System.err.print("Could not write file $filePath: ${e.message}")
        }
    }

    private companion object {
        private const val KEY_MAJOR = "MAJOR"
        private const val KEY_MINOR = "MINOR"
        private const val KEY_PATCH = "PATCH"
        private const val KEY_SUFFIX = "SUFFIX"
    }
}