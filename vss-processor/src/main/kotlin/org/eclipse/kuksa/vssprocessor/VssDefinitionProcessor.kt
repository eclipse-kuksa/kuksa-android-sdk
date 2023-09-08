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
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
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
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class VssDefinitionProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val visitor = VssDefinitionVisitor()
    private val yamlParser = YamlParser()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(VssDefinition::class.qualifiedName.toString())
        val deferredSymbols = symbols.filterNot { it.validate() }

        symbols.forEach { it.accept(visitor, Unit) }

        return deferredSymbols.toList()
    }

    inner class VssDefinitionVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val containingFile = classDeclaration.containingFile ?: return

            codeGenerator.createNewFile(
                Dependencies(false, containingFile),
                PACKAGE_NAME,
                DEFAULT_FILE_NAME,
            )

            val vssDefinition = classDeclaration.getAnnotationsByType(VssDefinition::class).first()
            val vssDefinitionPath = vssDefinition.vssDefinitionPath

            val definitionFile = loadAssetFile(vssDefinitionPath)
            if (!definitionFile.exists()) {
                logger.error("No VSS definition file was found!")
                return
            }

            val simpleSpecificationElements = yamlParser.parseSpecifications(definitionFile)
            val specificationElements = mapChildSpecifications(simpleSpecificationElements)

            generateModelFiles(specificationElements)
        }

        // Uses the default file path for generated files (from the code generator) and searches for the given file.
        private fun loadAssetFile(fileName: String): File {
            val generationPath = codeGenerator.generatedFile.first().absolutePath
            val buildPath = generationPath.replaceAfter(BUILD_FOLDER_NAME, "")
            val assetsFilePath = "$buildPath/$ASSETS_BUILD_DIRECTORY"
            val assetsFolder = File(assetsFilePath)

            return assetsFolder.walk().first { it.name == fileName }
        }

        // Takes all defined elements and nests them accordingly to their parents / child depending on the vssPath.
        // Map<ParentVssPath, ChildElements>
        private fun mapChildSpecifications(
            specificationElements: List<VssSpecificationElement>,
        ): Map<VssPath, VssSpecificationElement> {
            val vssPathToElement = specificationElements.associateBy({ VssPath(it.vssPath) }, { it })
            for (element in specificationElements) {
                val parentKey = VssPath(element.parentVssPath)
                val parentSpecification = vssPathToElement[parentKey] ?: continue

                parentSpecification.childSpecifications.add(element) // It must be a child of the parent
            }

            return vssPathToElement
        }

        private fun generateModelFiles(vssPathToSpecification: Map<VssPath, VssSpecificationElement>) {
            val duplicateSpecificationNames = vssPathToSpecification.keys
                .groupBy { it.leaf }
                .filter { it.value.size > 1 }
                .keys

            logger.logging("Ambiguous specifications - Generate nested classes: $duplicateSpecificationNames")

            for ((vssPath, vssSpecification) in vssPathToSpecification) {
                // Every duplicate is produced as a nested class - No separate file should be generated
                if (duplicateSpecificationNames.contains(vssPath.leaf)) {
                    continue
                }

                val classSpec = createSpecificationClassSpec(vssSpecification, duplicateSpecificationNames)

                val file = FileSpec.builder(PACKAGE_NAME, classSpec.name!!)
                    .addType(classSpec)
                    .build()

                file.writeTo(codeGenerator, false)
            }
        }

        private fun createSpecificationClassSpec(
            vssSpecification: VssSpecificationElement,
            nestedLeafNames: Set<String> = emptySet(),
        ): TypeSpec {
            val vssPath = VssPath(vssSpecification.vssPath)
            val vssLeafName = vssPath.leaf
            val childSpecifications = vssSpecification.childSpecifications

            val nestedChildSpecs = mutableListOf<TypeSpec>()
            val constructorBuilder = FunSpec.constructorBuilder()
            val propertySpecs = mutableListOf<PropertySpec>()
            val superInterfaces = mutableSetOf<TypeName>(VssSpecification::class.asTypeName())

            // The last element in the chain should have a value like "isLocked".
            if (childSpecifications.isEmpty()) {
                val (valuePropertySpec, parameterSpec) = createValueSpec(vssSpecification)

                constructorBuilder.addParameter(parameterSpec)
                propertySpecs.add(valuePropertySpec)

                // Final leafs should implement the vss property interface
                val vssPropertyInterface = VssProperty::class.asTypeName()
                    .plusParameter(vssSpecification.datatypeProperty)
                superInterfaces.add(vssPropertyInterface)
            }

            val propertySpec = createPropertySpecs(vssLeafName, vssSpecification)
            propertySpecs.addAll(propertySpec)

            // Parses all specifications into properties
            childSpecifications.forEach { childSpecification ->
                // This nested specification has an ambiguous name, add as nested class
                // The package name is different for nested classes (no qualifier)
                val hasAmbiguousName = nestedLeafNames.contains(childSpecification.name)
                val packageName = if (hasAmbiguousName) "" else PACKAGE_NAME

                val childPropertySpec = createPropertySpecs(vssLeafName, childSpecification, packageName)
                propertySpecs.addAll(childPropertySpec)

                // Nested VssSpecification properties should be added as constructor parameters
                val mainClassPropertySpec = childPropertySpec.first()
                if (mainClassPropertySpec.initializer != null) { // Only add a default for initializer
                    if (hasAmbiguousName) {
                        val childSpec = createSpecificationClassSpec(childSpecification, nestedLeafNames)
                        nestedChildSpecs.add(childSpec)
                    }

                    val defaultClassName = CLASS_NAME_PREFIX + childSpecification.name
                    val defaultParameter = createDefaultParameterSpec(
                        mainClassPropertySpec.name,
                        defaultClassName,
                        packageName,
                    )

                    constructorBuilder.addParameter(defaultParameter)
                }
            }

            val prefixedClassName = CLASS_NAME_PREFIX + vssLeafName
            val className = ClassName(PACKAGE_NAME, prefixedClassName)

            return TypeSpec.classBuilder(className)
                .addModifiers(KModifier.DATA)
                .primaryConstructor(constructorBuilder.build())
                .addSuperinterfaces(superInterfaces)
                .addProperties(propertySpecs)
                .addTypes(nestedChildSpecs)
                .build()
        }

        private fun createValueSpec(mainSpecification: VssSpecificationElement): Pair<PropertySpec, ParameterSpec> {
            val valuePropertySpec = PropertySpec
                .builder(PROPERTY_VALUE_NAME, mainSpecification.datatypeProperty)
                .initializer(PROPERTY_VALUE_NAME)
                .addModifiers(KModifier.OVERRIDE)
                .build()

            // Adds a default value (mainly 0 or an empty string)
            val parameterSpec = ParameterSpec.builder(
                valuePropertySpec.name,
                valuePropertySpec.type,
            ).defaultValue("%L", mainSpecification.defaultValue).build()
            return Pair(valuePropertySpec, parameterSpec)
        }

        private fun createDefaultParameterSpec(
            parameterName: String,
            defaultClassName: String,
            packageName: String = PACKAGE_NAME,
        ) = ParameterSpec
            .builder(parameterName, ClassName(packageName, defaultClassName))
            .defaultValue("%L()", defaultClassName)
            .build()

        private fun createPropertySpecs(
            className: String,
            specification: VssSpecificationElement,
            packageName: String = PACKAGE_NAME,
        ): List<PropertySpec> {
            val propertySpecs = mutableListOf<PropertySpec>()
            val members = VssSpecification::class.memberProperties

            fun createPrimitiveDataTypeSpec(member: KProperty1<VssSpecification, *>): PropertySpec {
                val memberName = member.name
                val memberType = member.returnType.asTypeName()
                val initialValue = member.get(specification) ?: ""

                return PropertySpec
                    .builder(memberName, memberType)
                    .mutable(false)
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return %S", initialValue)
                            .build(),
                    ).build()
            }

            fun createObjectTypeSpec(
                typeName: String,
                variableName: String,
                packageName: String = PACKAGE_NAME,
            ): PropertySpec {
                val prefixedTypeName = ClassName(packageName, CLASS_NAME_PREFIX + typeName)
                return PropertySpec
                    .builder(variableName, prefixedTypeName)
                    .initializer(variableName)
                    .build()
            }

            // Add primitive data types
            if (specification.name == className) {
                members.forEach { member ->
                    val primitiveDataTypeSpec = createPrimitiveDataTypeSpec(member)
                    propertySpecs.add(primitiveDataTypeSpec)
                }

                return propertySpecs
            }

            // Add nested child classes
            // Fixes duplicates e.g. type as variable and nested type
            val specificationName = specification.name
            val isVariableOccupied = members.find { member ->
                member.name.equals(specificationName, true)
            }
            val variablePrefix = if (isVariableOccupied != null) CLASS_NAME_PREFIX else ""
            val variableName = (variablePrefix + specificationName)
                .replaceFirstChar { it.lowercase() }

            val objectTypeSpec = createObjectTypeSpec(specificationName, variableName, packageName)
            return listOf(objectTypeSpec)
        }

        private fun generateEnumFile(name: String, enumNames: Set<String>) {
            if (enumNames.isEmpty()) {
                logger.warn("No enum elements found for generating!")
                return
            }

            val enumBuilder = TypeSpec.enumBuilder(name)
            enumNames.forEach { type ->
                enumBuilder.addEnumConstant(type)
            }

            val file = FileSpec.builder(PACKAGE_NAME, name)
                .addType(enumBuilder.build())
                .build()
            file.writeTo(codeGenerator, false)
        }
    }

    class YamlParser {
        fun parseSpecifications(definitionFile: File): List<VssSpecificationElement> {
            val specificationElements = mutableListOf<VssSpecificationElement>()
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
        private fun parseYamlElement(yamlElement: List<String>): VssSpecificationElement {
            val elementVssPath = yamlElement.first().substringBefore(":")

            val yamlElementJoined = yamlElement
                .joinToString(separator = ";")
                .substringAfter(";") // Remove vssPath (already parsed)
                .prependIndent(";") // So the parsing is consistent for the first element
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
                    .substringAfter(";$memberName: ") // Also parse "," to not confuse type != datatype
                    .substringBefore(";")

                val fieldInfo = Pair(memberName, memberValue)
                fieldsToSet.add(fieldInfo)
            }

            val vssSpecificationMember = VssSpecificationElement()
            vssSpecificationMember.setFields(fieldsToSet)

            return vssSpecificationMember
        }

        private fun Any.setFields(
            fields: MutableList<Pair<String, Any?>>,
            remapNames: Map<String, String> = emptyMap(),
        ) {
            val nameToProperty = this::class.memberProperties.associateBy(KProperty<*>::name)
            remapNames.forEach { (propertyName, newName) ->
                val find = fields.find { it.first == propertyName } ?: return@forEach
                fields.remove(find)
                fields.add(Pair(find.first, newName))
            }

            fields.forEach { (propertyName, propertyValue) ->
                nameToProperty[propertyName]
                    .takeIf { it is KMutableProperty<*> }
                    ?.let { it as KMutableProperty<*> }
                    ?.setter?.call(this, propertyValue)
            }
        }
    }

    data class VssPath(val path: String) {
        val leaf: String
            get() = path.substringAfterLast(".")
    }

    // Reflects the specification file as a data model and is filled via reflection. That is why the variable names
    // should exactly match the names inside the specification file and be of a string type.
    data class VssSpecificationElement(
        override var uuid: String = "",
        override var vssPath: String = "",
        override var description: String = "",
        override var type: String = "",
        override var comment: String = "",
        var datatype: String = "",
    ) : VssSpecification {
        var childSpecifications = mutableListOf<VssSpecificationElement>()

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
         * Returns valid default values as string literals.
         */
        val defaultValue: String?
            get() {
                return when (datatypeProperty) {
                    String::class.asTypeName() -> "\"\""
                    Boolean::class.asTypeName() -> "false"
                    Float::class.asTypeName() -> "0f"
                    Array::class.parameterizedBy(String::class) -> "emptyArray<String>()"
                    Double::class.asTypeName() -> "0.0"
                    Int::class.asTypeName() -> "0"
                    UInt::class.asTypeName() -> "0u"
                    IntArray::class.asTypeName() -> "IntArray(0)"
                    BooleanArray::class.asTypeName() -> "BooleanArray(0)"

                    else -> null
                }
            }

        /**
         * Returns the parent key depending on the [vssPath].
         */
        val parentKey: String
            get() {
                val keys = specificationKeys
                if (keys.size < 2) return ""

                return keys[keys.size - 2]
            }

        /**
         * Return the parent [vssPath].
         */
        val parentVssPath: String
            get() = vssPath.substringBeforeLast(".", "")

        /**
         * Splits the vssPath into its parts.
         */
        private val specificationKeys: List<String>
            get() = vssPath.split(".")

        override fun equals(other: Any?): Boolean {
            if (other !is VssSpecificationElement) return false

            return uuid == other.uuid
        }

        override fun hashCode(): Int {
            return uuid.hashCode()
        }
    }

    companion object {
        private const val PACKAGE_NAME = "org.eclipse.kuksa.vss"
        private const val DEFAULT_FILE_NAME = "VssProcessor"
        private const val PROPERTY_VALUE_NAME = "value"
        private const val CLASS_NAME_PREFIX = "Vss"
        private const val ASSETS_BUILD_DIRECTORY = "intermediates/assets/"
        private const val BUILD_FOLDER_NAME = "build/"
    }
}

class VssDefinitionProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return VssDefinitionProcessor(environment.codeGenerator, environment.logger)
    }
}
