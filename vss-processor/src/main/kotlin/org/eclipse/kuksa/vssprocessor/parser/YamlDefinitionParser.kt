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

package org.eclipse.kuksa.vssprocessor.parser

import org.eclipse.kuksa.vsscore.model.VssSpecification
import org.eclipse.kuksa.vssprocessor.spec.VssSpecificationSpecModel
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

internal class YamlDefinitionParser : VssDefinitionParser {
    override fun parseSpecifications(definitionFile: File, elementDelimiter: String): List<VssSpecificationSpecModel> {
        val specificationElements = mutableListOf<VssSpecificationSpecModel>()
        definitionFile.useLines { lines ->
            val yamlAttributes = mutableListOf<String>()
            for (line in lines.toList()) {
                val trimmedLine = line.trim()
                if (trimmedLine == elementDelimiter) { // A new element will follow after the delimiter
                    parseYamlElement(yamlAttributes)?.let { specificationElement ->
                        specificationElements.add(specificationElement)
                    }

                    yamlAttributes.clear()

                    continue
                }

                yamlAttributes.add(trimmedLine)
            }

            // Add the last element because no empty line will follow
            parseYamlElement(yamlAttributes)?.let { specificationElement ->
                specificationElements.add(specificationElement)
            }
        }

        return specificationElements
    }

    // Example .yaml element:
    //
    // Vehicle.ADAS.ABS:
    //  description: Antilock Braking System signals.
    //  type: branch
    //  uuid: 219270ef27c4531f874bbda63743b330
    private fun parseYamlElement(yamlElement: List<String>, delimiter: Char = ';'): VssSpecificationSpecModel? {
        val elementVssPath = yamlElement.first().substringBefore(":")

        val yamlElementJoined = yamlElement
            .joinToString(separator = delimiter.toString())
            .substringAfter(delimiter) // Remove vssPath (already parsed)
            .prependIndent(delimiter.toString()) // So the parsing is consistent for the first element
        val members = VssSpecificationSpecModel::class.memberProperties
        val fieldsToSet = mutableListOf<Pair<String, Any?>>()

        // The VSSPath is an exception because it is parsed from the top level name.
        val vssPathFieldInfo = Pair("vssPath", elementVssPath)
        fieldsToSet.add(vssPathFieldInfo)

        // Parse (example: "description: Antilock Braking System signals.") into name + value for all .yaml lines
        for (member in members) {
            val memberName = member.name
            if (!yamlElementJoined.contains(memberName)) continue

            // Also parse the delimiter to not confuse type != datatype
            val memberValue = yamlElementJoined
                .substringAfter("$delimiter$memberName: ")
                .substringBefore(delimiter)

            val fieldInfo = Pair(memberName, memberValue)
            fieldsToSet.add(fieldInfo)
        }

        val vssSpecificationMember = VssSpecificationSpecModel()
        vssSpecificationMember.setFields(fieldsToSet)

        if (vssSpecificationMember.uuid.isEmpty()) return null

        return vssSpecificationMember
    }
}

/**
 * @param fields to set via reflection. Pair<PropertyName, anyValue>.
 * @param remapNames which can be used if the propertyName does not match with the input name
 */
private fun VssSpecification.setFields(
    fields: List<Pair<String, Any?>>,
    remapNames: Map<String, String> = emptyMap(),
) {
    val nameToProperty = this::class.memberProperties.associateBy(KProperty<*>::name)

    val remappedFields = fields.toMutableList()
    remapNames.forEach { (propertyName, newName) ->
        val find = fields.find { it.first == propertyName } ?: return@forEach
        remappedFields.remove(find)
        remappedFields.add(Pair(find.first, newName))
    }

    remappedFields.forEach { (propertyName, propertyValue) ->
        nameToProperty[propertyName]
            .takeIf { it is KMutableProperty<*> }
            ?.let { it as KMutableProperty<*> }
            ?.setter?.call(this, propertyValue)
    }
}
