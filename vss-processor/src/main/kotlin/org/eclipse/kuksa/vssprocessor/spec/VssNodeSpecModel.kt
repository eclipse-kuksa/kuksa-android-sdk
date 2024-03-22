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

package org.eclipse.kuksa.vssprocessor.spec

import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.eclipse.kuksa.vsscore.model.VssBranch
import org.eclipse.kuksa.vsscore.model.VssNode
import org.eclipse.kuksa.vsscore.model.VssSignal
import org.eclipse.kuksa.vsscore.model.className
import org.eclipse.kuksa.vsscore.model.name
import org.eclipse.kuksa.vsscore.model.parentClassName
import org.eclipse.kuksa.vsscore.model.parentKey
import org.eclipse.kuksa.vsscore.model.variableName
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_COMMENT
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_DATATYPE
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_DESCRIPTION
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_TYPE
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_UUID
import org.eclipse.kuksa.vssprocessor.spec.VssNodeProperty.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

internal class VssNodeSpecModel(
    override var vssPath: String = "",
    val vssNodeProperties: Set<VssNodeProperty>,
) : VssNode, SpecModel<VssNodeSpecModel> {

    private val propertyNameToNodePropertyMap = vssNodeProperties.associateBy { it.nodePropertyName }

    override var uuid: String = propertyNameToNodePropertyMap[KEY_DATA_UUID]?.nodePropertyValue ?: ""
    override var type: String = propertyNameToNodePropertyMap[KEY_DATA_TYPE]?.nodePropertyValue ?: ""
    override var description: String = propertyNameToNodePropertyMap[KEY_DATA_DESCRIPTION]?.nodePropertyValue ?: ""
    override var comment: String = propertyNameToNodePropertyMap[KEY_DATA_COMMENT]?.nodePropertyValue ?: ""
    var datatype: String = propertyNameToNodePropertyMap[KEY_DATA_DATATYPE]?.nodePropertyValue ?: ""

    var logger: KSPLogger? = null

    private val stringTypeName = String::class.asTypeName()
    private val vssNodeSetTypeName = Set::class.parameterizedBy(VssNode::class)
    private val genericClassTypeNameNullable = KClass::class.asClassName().parameterizedBy(STAR).copy(nullable = true)

    private val vssDataType by lazy { VssDataType.find(datatype) }

    private val datatypeTypeName: TypeName
        get() {
            return when (vssDataType) {
                VssDataType.STRING_ARRAY -> vssDataType.dataType.parameterizedBy(String::class)
                else -> vssDataType.dataType.asTypeName()
            }
        }

    private val valueTypeName: TypeName
        get() {
            return when (vssDataType) {
                VssDataType.STRING_ARRAY -> vssDataType.valueDataType.parameterizedBy(String::class)
                else -> vssDataType.valueDataType.asTypeName()
            }
        }

    private val defaultValue: String
        get() = vssDataType.defaultValue

    override fun createClassSpec(
        packageName: String,
        relatedNodes: Collection<VssNodeSpecModel>,
        nestedClasses: Collection<String>,
    ): TypeSpec {
        val childNodes = relatedNodes.filter { it.parentKey == name }
        // Every specification has only one parent, do not check again for heirs
        val reducedRelatedNodes = relatedNodes - childNodes.toSet() - this

        val nestedChildSpecs = mutableListOf<TypeSpec>()
        val constructorBuilder = FunSpec.constructorBuilder()
            .addAnnotation(JvmOverloads::class)
        val propertySpecs = mutableListOf<PropertySpec>()
        val superInterfaces = mutableSetOf<TypeName>(VssBranch::class.asTypeName())

        // The last element in the chain should have a value like "isLocked".
        val isVssSignal = childNodes.isEmpty()
        if (isVssSignal) {
            val (vssSignalTypeName, vssSignalPropertySpecs, vssSignalParameterSpecs) = createVssSignalSpec()

            // Final leafs should ONLY implement the VssSignal interface
            superInterfaces.clear()
            superInterfaces.add(vssSignalTypeName)

            propertySpecs.addAll(vssSignalPropertySpecs)
            vssSignalParameterSpecs.forEach {
                constructorBuilder.addParameter(it)
            }
        }

        val propertySpec = createVssNodeSpecs(className, packageName = packageName)
        propertySpecs.addAll(propertySpec)

        // Parses all VssNodes into properties
        childNodes.forEach { childNode ->
            // This nested node has an ambiguous name, add as nested class
            // The package name is different for nested classes (no qualifier)
            val hasAmbiguousName = nestedClasses.contains(childNode.name)
            val uniquePackageName = if (hasAmbiguousName) "" else packageName

            val childPropertySpec = createVssNodeSpecs(className, uniquePackageName, childNode)
            propertySpecs.addAll(childPropertySpec)

            // Nested VssNode properties should be added as constructor parameters
            val mainClassPropertySpec = childPropertySpec.first()
            if (mainClassPropertySpec.initializer != null) { // Only add a default for initializer
                if (hasAmbiguousName) {
                    // All children should contain the prefix vssPath (improves performance)
                    val relevantRelatedNodes = reducedRelatedNodes.filter {
                        it.vssPath.contains(childNode.vssPath + ".")
                    }
                    val childSpec = childNode.createClassSpec(
                        packageName,
                        relevantRelatedNodes,
                        nestedClasses,
                    )

                    nestedChildSpecs.add(childSpec)
                }

                val defaultClassName = childNode.className
                val defaultParameter = ParameterSpec
                    .builder(mainClassPropertySpec.name, ClassName(uniquePackageName, defaultClassName))
                    .defaultValue("%L()", defaultClassName)
                    .build()

                constructorBuilder.addParameter(defaultParameter)
            }
        }

        val nodeSpecs = createVssNodeTreeSpecs(childNodes)
        propertySpecs.addAll(nodeSpecs)

        val className = ClassName(packageName, className)

        return TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(constructorBuilder.build())
            .addSuperinterfaces(superInterfaces)
            .addProperties(propertySpecs)
            .addTypes(nestedChildSpecs)
            .build()
    }

    private fun createVssSignalSpec(): Triple<ParameterizedTypeName, List<PropertySpec>, List<ParameterSpec>> {
        fun getReturnStatement(propertyClass: KClass<*>): String {
            val placeholder = when (propertyClass) {
                String::class -> "%S"
                Float::class -> "%L.toFloat()"
                Double::class -> "%L.toDouble()"
                else -> "%L"
            }

            return "return $placeholder"
        }

        val propertySpecs = mutableListOf<PropertySpec>()
        val parameterSpecs = mutableListOf<ParameterSpec>()

        val (classPropertySpec, classParameterSpec) = createClassParamSpec(
            VssSignal<*>::value.name,
            valueTypeName,
            defaultValue,
        )
        parameterSpecs.add(classParameterSpec)
        propertySpecs.add(classPropertySpec)

        val genericClassSpec = createGenericClassSpec(
            VssSignal<*>::dataType.name,
            VssSignal<*>::dataType.returnType.asTypeName(),
            datatypeTypeName.toString(),
        )
        propertySpecs.add(genericClassSpec)
        val vssSignalProperties = vssNodeProperties
            .filter {
                !it.isCommon && it.nodePropertyName != VssSignal<*>::dataType.name.lowercase()
            }

        vssSignalProperties.forEach { vssSignalProperty ->
            val value = vssSignalProperty.nodePropertyValue
            if (value.isEmpty()) return@forEach

            val propertyName = vssSignalProperty.nodePropertyName.lowercase()
            val propertyType = vssSignalProperty.dataType
            val returnStatement = getReturnStatement(vssSignalProperty.dataType)
            val propertySpec = PropertySpec
                .builder(propertyName, propertyType)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement(returnStatement, value)
                        .build(),
                )
                .build()
            propertySpecs.add(propertySpec)
        }

        val typeName = VssSignal::class
            .asTypeName()
            .plusParameter(valueTypeName)

        return Triple(typeName, propertySpecs, parameterSpecs)
    }

    private fun createClassParamSpec(
        memberName: String,
        typeName: TypeName,
        defaultValue: String,
    ): Pair<PropertySpec, ParameterSpec> {
        val propertySpec = PropertySpec
            .builder(memberName, typeName)
            .initializer(memberName)
            .addModifiers(KModifier.OVERRIDE)
            .build()

        // Adds a default value (mainly 0 or an empty string)
        val parameterSpec = ParameterSpec.builder(
            memberName,
            typeName,
        ).defaultValue("%L", defaultValue).build()

        return Pair(propertySpec, parameterSpec)
    }

    private fun createVssNodeSpecs(
        className: String,
        packageName: String,
        specModel: VssNodeSpecModel = this,
    ): List<PropertySpec> {
        val propertySpecs = mutableListOf<PropertySpec>()
        val members = VssNode::class.declaredMemberProperties

        fun createInterfaceDataTypeSpec(member: KProperty1<VssNode, *>): PropertySpec {
            val memberName = member.name
            val memberType = member.returnType.asTypeName()
            val initialValue = member.get(specModel) ?: ""

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
            nodeSpecModel: VssNodeSpecModel,
            packageName: String,
        ): PropertySpec {
            val prefixedTypeName = ClassName(packageName, nodeSpecModel.className)
            val variableName = nodeSpecModel.variableName

            return PropertySpec
                .builder(variableName, prefixedTypeName)
                .initializer(variableName)
                .build()
        }

        // Add primitive data types
        if (specModel.className == className) {
            members.forEach { member ->
                when (member.returnType.asTypeName()) {
                    stringTypeName -> createInterfaceDataTypeSpec(member)
                    else -> null
                }?.let { primitiveDataTypeSpec ->
                    propertySpecs.add(primitiveDataTypeSpec)
                }
            }

            return propertySpecs
        }

        // Add nested child classes
        val objectTypeSpec = createObjectTypeSpec(specModel, packageName)
        return listOf(objectTypeSpec)
    }

    private fun createVssNodeTreeSpecs(childNodes: List<VssNodeSpecModel>): List<PropertySpec> {
        val propertySpecs = mutableListOf<PropertySpec>()

        val members = VssNode::class.declaredMemberProperties
        members.forEach { member ->
            val memberName = member.name
            when (val memberType = member.returnType.asTypeName()) {
                vssNodeSetTypeName -> createSetSpec(memberName, memberType, childNodes)
                genericClassTypeNameNullable -> createGenericClassSpec(memberName, memberType, parentClassName)
                else -> null
            }?.let { propertySpec ->
                propertySpecs.add(propertySpec)
            }
        }

        return propertySpecs
    }

    private fun createGenericClassSpec(memberName: String, memberType: TypeName, className: String): PropertySpec {
        val parentClass = if (className.isNotEmpty()) "$className::class" else "null"

        val propertySpecBuilder = PropertySpec
            .builder(memberName, memberType)

        // Removed the warning about ExperimentalUnsignedTypes
        if (experimentalUnsignedTypes.contains(className)) {
            val optInClassName = ClassName("kotlin", "OptIn")
            val optInAnnotationSpec = AnnotationSpec.builder(optInClassName)
                .addMember("ExperimentalUnsignedTypes::class")
                .build()

            propertySpecBuilder.addAnnotation(optInAnnotationSpec)
        }

        return propertySpecBuilder
            .addModifiers(KModifier.OVERRIDE)
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("return %L", parentClass)
                    .build(),
            )
            .build()
    }

    private fun createSetSpec(
        memberName: String,
        memberType: TypeName,
        members: Collection<VssNodeSpecModel>,
    ): PropertySpec {
        val vssNodeNamesJoined = members.joinToString(", ") { it.variableName }

        return PropertySpec
            .builder(memberName, memberType)
            .addModifiers(KModifier.OVERRIDE)
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("return setOf(%L)", vssNodeNamesJoined)
                    .build(),
            )
            .build()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is VssNodeSpecModel) return false

        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    override fun toString(): String {
        return vssPath
    }

    companion object {
        private val experimentalUnsignedTypes = setOf("kotlin.UIntArray")
    }
}

internal data class VssPath(val path: String) {
    val leaf: String
        get() = path.substringAfterLast(".")
}
