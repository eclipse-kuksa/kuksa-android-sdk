/*
 * Copyright (c) 2023 - 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.kuksa.vssprocessor.parser.yaml

import org.eclipse.kuksa.vssprocessor.parser.FileParseException
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_COMMENT
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_DATATYPE
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_DESCRIPTION
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_MAX
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_MIN
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_TYPE
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_UNIT
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_UUID
import org.eclipse.kuksa.vssprocessor.parser.VssParser
import org.eclipse.kuksa.vssprocessor.spec.VssDataType
import org.eclipse.kuksa.vssprocessor.spec.VssNodeProperty
import org.eclipse.kuksa.vssprocessor.spec.VssNodeSpecModel
import org.eclipse.kuksa.vssprocessor.spec.VssSignalProperty
import java.io.File
import java.io.IOException

internal class YamlVssParser(private val elementDelimiter: String = "") : VssParser {
    override fun parseNodes(vssFile: File): List<VssNodeSpecModel> {
        val vssNodeElements = mutableListOf<VssNodeSpecModel>()
        try {
            vssFile.useLines { lines ->
                val yamlAttributes = mutableListOf<String>()
                for (line in lines.toList()) {
                    val trimmedLine = line.trim()
                    if (trimmedLine == elementDelimiter) { // A new element will follow after the delimiter
                        parseYamlElement(yamlAttributes).let { element ->
                            vssNodeElements.add(element)
                        }

                        yamlAttributes.clear()

                        continue
                    }

                    yamlAttributes.add(trimmedLine)
                }

                // Add the last element because no empty line will follow
                parseYamlElement(yamlAttributes).let { element ->
                    vssNodeElements.add(element)
                }
            }
        } catch (e: FileParseException) {
            throw IOException("Invalid VSS File: '${vssFile.path}'", e)
        }

        return vssNodeElements
    }

    // Example .yaml element:
    //
    // Vehicle.ADAS.ABS:
    //  description: Antilock Braking System signals.
    //  type: branch
    //  uuid: 219270ef27c4531f874bbda63743b330
    private fun parseYamlElement(yamlElement: List<String>, delimiter: Char = ';'): VssNodeSpecModel {
        val vssPath = yamlElement.first().substringBefore(":")

        val yamlElementJoined = yamlElement
            .joinToString(separator = delimiter.toString())
            .substringAfter(delimiter) // Remove vssPath (already parsed)
            .prependIndent(delimiter.toString()) // So the parsing is consistent for the first element

        // The VSSPath is an exception because it is parsed from the top level name.

        // Parse (example: "description: Antilock Braking System signals.") into name + value for all .yaml lines
        val uuid = fetchValue(KEY_DATA_UUID, yamlElementJoined, delimiter)
            ?: throw FileParseException("Could not parse '$KEY_DATA_UUID' for '$vssPath'")

        val type = fetchValue(KEY_DATA_TYPE, yamlElementJoined, delimiter)
            ?: throw FileParseException("Could not parse '$KEY_DATA_TYPE' for '$vssPath'")

        val description = fetchValue(KEY_DATA_DESCRIPTION, yamlElementJoined, delimiter) ?: ""
        val datatype = fetchValue(KEY_DATA_DATATYPE, yamlElementJoined, delimiter) ?: ""
        val comment = fetchValue(KEY_DATA_COMMENT, yamlElementJoined, delimiter) ?: ""
        val unit = fetchValue(KEY_DATA_UNIT, yamlElementJoined, delimiter) ?: ""
        val min = fetchValue(KEY_DATA_MIN, yamlElementJoined, delimiter) ?: ""
        val max = fetchValue(KEY_DATA_MAX, yamlElementJoined, delimiter) ?: ""

        val vssDataType = VssDataType.find(datatype)
        val valueDataType = vssDataType.valueDataType

        val vssNodeProperties = mutableSetOf(
            VssNodeProperty(vssPath, KEY_DATA_UUID, uuid, String::class),
            VssNodeProperty(vssPath, KEY_DATA_TYPE, type, String::class),
            VssNodeProperty(vssPath, KEY_DATA_DESCRIPTION, description, String::class),
            VssNodeProperty(vssPath, KEY_DATA_COMMENT, comment, String::class),
            VssSignalProperty(vssPath, KEY_DATA_DATATYPE, datatype, valueDataType),
            VssSignalProperty(vssPath, KEY_DATA_UNIT, unit, String::class),
            VssSignalProperty(vssPath, KEY_DATA_MIN, min, valueDataType),
            VssSignalProperty(vssPath, KEY_DATA_MAX, max, valueDataType),
        )

        return VssNodeSpecModel(vssPath, vssNodeProperties)
    }
}

private fun fetchValue(
    nodeName: String,
    yamlElementJoined: String,
    delimiter: Char,
): String? {
    // Also parse the delimiter to not confuse type != datatype
    val value = yamlElementJoined
        .substringAfter("$delimiter$nodeName: ")
        .substringBefore(delimiter)

    return value.ifEmpty { return null }
}
