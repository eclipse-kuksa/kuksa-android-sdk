package org.eclipse.kuksa.vssprocessor.parser

import org.eclipse.kuksa.vssprocessor.spec.VssSpecificationSpecModel
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.reflect.full.memberProperties

internal class YamlDefinitionParser : VssDefinitionParser {
    override fun parseSpecifications(definitionFile: File): List<VssSpecificationSpecModel> {
        val specificationElements = mutableListOf<VssSpecificationSpecModel>()
        val vssDefinitionStream = definitionFile.inputStream()
        val bufferedReader = BufferedReader(InputStreamReader(vssDefinitionStream))

        val yamlAttributes = mutableListOf<String>()
        while (bufferedReader.ready()) {
            val line = bufferedReader.readLine().trim()
            if (line.isEmpty()) {
                val specificationElement = parseYamlElement(yamlAttributes)
                specificationElements.add(specificationElement)

                yamlAttributes.clear()

                continue
            }

            yamlAttributes.add(line)
        }

        bufferedReader.close()
        vssDefinitionStream.close()

        return specificationElements
    }

    // Example .yaml element:
    //
    // Vehicle.ADAS.ABS:
    //  description: Antilock Braking System signals.
    //  type: branch
    //  uuid: 219270ef27c4531f874bbda63743b330
    private fun parseYamlElement(yamlElement: List<String>): VssSpecificationSpecModel {
        val elementVssPath = yamlElement.first().substringBefore(":")

        val yamlElementJoined = yamlElement
            .joinToString(separator = ";")
            .substringAfter(";") // Remove vssPath (already parsed)
            .prependIndent(";") // So the parsing is consistent for the first element
        val members = VssSpecificationSpecModel::class.memberProperties
        val fieldsToSet = mutableListOf<Pair<String, Any?>>()

        // The VSSPath is an exception because it is parsed from the top level name.
        val vssPathFieldInfo = Pair("vssPath", elementVssPath)
        fieldsToSet.add(vssPathFieldInfo)

        // Parse (example: "description: Antilock Braking System signals.") into name + value for all .yaml lines
        for (member in members) {
            val memberName = member.name
            if (!yamlElementJoined.contains(memberName)) continue

            val memberValue = yamlElementJoined
                .substringAfter(";$memberName: ") // Also parse "," to not confuse type != datatype
                .substringBefore(";")

            val fieldInfo = Pair(memberName, memberValue)
            fieldsToSet.add(fieldInfo)
        }

        val vssSpecificationMember = VssSpecificationSpecModel()
        vssSpecificationMember.setFields(fieldsToSet)

        return vssSpecificationMember
    }
}
