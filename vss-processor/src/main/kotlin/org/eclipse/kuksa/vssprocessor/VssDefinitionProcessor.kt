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

@file:OptIn(KspExperimental::class)

package org.eclipse.kuksa.vssprocessor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

class VssDefinitionProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val visitor = VssDefinitionVisitor()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(VssDefinition::class.qualifiedName.toString())
        symbols.forEach { it.accept(visitor, Unit) }

        return emptyList()
    }

    inner class VssDefinitionVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            codeGenerator.createNewFile(
                Dependencies(false, classDeclaration.containingFile!!),
                PACKAGE_NAME,
                "VSSProcessor",
            )

            val vssDefinition = classDeclaration.getAnnotationsByType(VssDefinition::class).first()
            val vssDefinitionPath = vssDefinition.vssDefinitionPath

            val generationPath = codeGenerator.generatedFile.first().absolutePath
            val buildPath = generationPath.replaceAfter("build/", "")
            val assetsFilePath = "$buildPath/intermediates/assets/"
            val assetsFolder = File(assetsFilePath)
            val definitionFile = assetsFolder.walk().first { it.name == vssDefinitionPath }

            if (!definitionFile.exists()) {
                logger.error("No VSS definition file was found!")
                return
            }

            val simpleSpecificationElements = parseSpecifications(definitionFile)
            val specificationElements = fillChildSpecifications(simpleSpecificationElements)

            generateModels(specificationElements)
        }

        // The key of the map is the class name for specification
        private fun parseSpecifications(definitionFile: File): List<VssSpecificationElement> {
            val specificationElements = mutableListOf<VssSpecificationElement>()
            val vssDefinitionStream = definitionFile.inputStream()
            val bufferedReader = BufferedReader(InputStreamReader(vssDefinitionStream))

            val yamlAttributes = mutableListOf<String>()
            while (bufferedReader.ready()) {
                val line = bufferedReader.readLine().trim()
                if (line.isEmpty()) {
                    val specificationElement = parseYamlElement(yamlAttributes)
                    specificationElements.add(specificationElement)

                    logger.logging("Found next yaml Element: $specificationElements")

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
        private fun parseYamlElement(yamlElement: List<String>): VssSpecificationElement {
            val elementVssPath = yamlElement.first().substringBefore(":")

            val yamlElementJoined = yamlElement
                .joinToString(separator = ",")
                .substringAfter(",") // Remove vssPath (already parsed)
                .prependIndent(",") // So the parsing is consistent for the first element
            val members = VssSpecificationElement::class.memberProperties
            val fieldsToSet = mutableListOf<Pair<String, Any?>>()

            // The VSSPath is an exception because it is parsed from the top level name.
            val vssPathFieldInfo = Pair("vssPath", elementVssPath)
            fieldsToSet.add(vssPathFieldInfo)

            // Parse (example: "description: Antilock Braking System signals.") into name + value for all .yaml lines
            for (member in members) {
                val memberName = member.name
                if (!yamlElementJoined.contains(memberName)) continue

                val memberValue = yamlElementJoined
                    .substringAfter(",$memberName: ") // Also parse "," to not confuse type != datatype
                    .substringBefore(",")

                val fieldInfo = Pair(memberName, memberValue)
                fieldsToSet.add(fieldInfo)
            }

            val vssSpecificationMember = VssSpecificationElement()
            vssSpecificationMember.setFields(fieldsToSet)

            return vssSpecificationMember
        }

        // Takes all defined elements and nests them accordingly to their parents / child depending on the vssPath.
        // Map<ParentClassName, ChildElements>
        private fun fillChildSpecifications(
            specificationElements: List<VssSpecificationElement>,
        ): Map<String, Set<VssSpecificationElement>> {
            val vssPathToElements = specificationElements.associateBy({ it.name }, { mutableSetOf(it) })
            for (element in specificationElements) {
                val parentKey = element.parentKey
                val parentSpecifications = vssPathToElements[parentKey] ?: continue

                parentSpecifications.add(element) // It must be a child of the parent
            }

            return vssPathToElements
        }

        private fun generateModels(classToSpecifications: Map<String, Set<VssSpecificationElement>>) {
            val fileBuilder = FileSpec.builder(PACKAGE_NAME, FILE_NAME)
            val typeEnums = mutableSetOf<String>()

            val finalProperties = classToSpecifications.filter { (_, vssSpecifications) ->
                vssSpecifications.size == 1
            }

            classToSpecifications.forEach { (className, vssSpecifications) ->
                val mainSpecification = vssSpecifications.first()
                typeEnums.add(mainSpecification.type.uppercase())

                val classBuilder = TypeSpec.classBuilder(className)
                    .addModifiers(KModifier.DATA)

                val constructorBuilder = FunSpec.constructorBuilder()
                val propertySpecs = mutableListOf<PropertySpec>()

                if (vssSpecifications.size == 1) {
                    val valuePropertySpec = PropertySpec
                        .builder("value", mainSpecification.datatypeProperty)
                        .initializer("value")
                        .build()
                    constructorBuilder.addParameter("value", mainSpecification.datatypeProperty)
                    propertySpecs.add(valuePropertySpec)
                }

                vssSpecifications.forEach { specification ->
                    val variableName = specification.vssPath.substringAfterLast(".")

                    if (variableName == className) {
                        val members = VssSpecification::class.memberProperties
                        members.forEach { member ->
                            val memberName = member.name
                            val memberType = member.returnType.asTypeName()
                            constructorBuilder.addParameter(memberName, memberType)

                            val propertySpec = PropertySpec
                                .builder(memberName, memberType)
                                .initializer(memberName)
                                .build()
                            propertySpecs.add(propertySpec)
                        }
                    } else {
                        constructorBuilder.addParameter(variableName, VssSpecification::class)

                        val propertySpec = PropertySpec
                            .builder(variableName, VssSpecification::class)
                            .initializer(variableName)
                            .build()
                        propertySpecs.add(propertySpec)
                    }
                }

                classBuilder.primaryConstructor(constructorBuilder.build()).build()
                classBuilder.addProperties(propertySpecs)
                fileBuilder.addType(classBuilder.build())
            }

            val enumBuilder = TypeSpec.enumBuilder("VSSType")
            typeEnums.forEach { type ->
                enumBuilder.addEnumConstant(type)
            }

            fileBuilder.addType(enumBuilder.build())

            val file = fileBuilder.build()
            file.writeTo(codeGenerator, false)
        }

        private fun Any.setFields(fields: List<Pair<String, Any?>>) {
            val nameToProperty = this::class.memberProperties.associateBy(KProperty<*>::name)
            fields.forEach { (propertyName, propertyValue) ->
                nameToProperty[propertyName]
                    .takeIf { it is KMutableProperty<*> }
                    ?.let { it as KMutableProperty<*> }
                    ?.setter?.call(this, propertyValue)
            }
        }
    }

    data class VssSpecificationElement(
        override var uuid: String = "",
        override var vssPath: String = "",
        override var description: String = "",
        override var type: String = "",
        var datatype: String = "",
    ) : VssSpecification {
        val name: String
            get() = vssPath.substringAfterLast(".")
        val datatypeProperty: TypeName
            get() {
                return when (datatype) {
                    "string" -> String::class.asTypeName()
                    "boolean" -> Boolean::class.asTypeName()
                    "uint8", "uint16", "uint32" -> UInt::class.asTypeName()
                    "int8", "int16", "int32" -> Int::class.asTypeName()
                    "float" -> Float::class.asTypeName()
                    "double" -> Double::class.asTypeName()
                    "string[]" -> Array::class.parameterizedBy(String::class)
                    "boolean[]" -> BooleanArray::class.asTypeName()
                    "uint8[]", "uint16[]", "uint32[]", "int8[]", "int16[]", "int32[]" -> IntArray::class.asTypeName()
                    else -> String::class.asTypeName()
                }
            }

        /**
         * Splits the vssPath into its parts.
         */
        private val specificationKeys: List<String>
            get() = vssPath.split(".")

        val parentKey: String
            get() {
                val keys = specificationKeys
                if (keys.size < 2) return ""

                return keys[keys.size - 2]
            }

        // We can't use the vssPath or the uuid because it will result into duplicates for the last property like
        // "isLocked". These children should be reused for multiple parents.
        override fun equals(other: Any?): Boolean {
            if (other !is VssSpecificationElement) return false

            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }

    companion object {
        private const val PACKAGE_NAME = "org.eclipse.kuksa.vss"
        private const val FILE_NAME = "VSSDefinitions"
    }
}

class VssDefinitionProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return VssDefinitionProcessor(environment.codeGenerator, environment.logger)
    }
}
