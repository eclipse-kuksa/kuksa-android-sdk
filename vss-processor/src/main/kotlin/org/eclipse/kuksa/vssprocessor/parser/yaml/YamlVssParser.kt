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
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.COMMENT
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.DATATYPE
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.DESCRIPTION
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.MAX
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.MIN
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.TYPE
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.UNIT
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.UUID
import org.eclipse.kuksa.vssprocessor.parser.VssParser
import org.eclipse.kuksa.vssprocessor.spec.VssNodePropertiesBuilder
import org.eclipse.kuksa.vssprocessor.spec.VssNodeSpecModel
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

        val uuid = fetchValue(UUID, yamlElementJoined, delimiter).ifEmpty {
            throw FileParseException("Could not parse '${UUID.key}' for '$vssPath'")
        }

        val type = fetchValue(TYPE, yamlElementJoined, delimiter).ifEmpty {
            throw FileParseException("Could not parse '${TYPE.key}' for '$vssPath'")
        }

        val description = fetchValue(DESCRIPTION, yamlElementJoined, delimiter)
        val comment = fetchValue(COMMENT, yamlElementJoined, delimiter)
        val datatype = fetchValue(DATATYPE, yamlElementJoined, delimiter)
        val unit = fetchValue(UNIT, yamlElementJoined, delimiter)
        val min = fetchValue(MIN, yamlElementJoined, delimiter)
        val max = fetchValue(MAX, yamlElementJoined, delimiter)

        val vssNodeProperties = VssNodePropertiesBuilder(uuid, type)
            .withDescription(description)
            .withComment(comment)
            .withDataType(datatype)
            .withUnit(unit)
            .withMin(min, datatype)
            .withMax(max, datatype)
            .build()

        return VssNodeSpecModel(vssPath, vssNodeProperties)
    }
}

private fun fetchValue(
    dataKey: VssDataKey,
    yamlElementJoined: String,
    delimiter: Char,
): String {
    // Also parse the delimiter to not confuse type != datatype
    return yamlElementJoined
        .substringAfter("$delimiter${dataKey.key}: ")
        .substringBefore(delimiter)
}
