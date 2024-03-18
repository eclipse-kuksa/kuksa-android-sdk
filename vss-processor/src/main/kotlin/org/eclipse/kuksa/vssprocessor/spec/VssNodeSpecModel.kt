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
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

// Reflects the VSS file as a model and is filled via reflection. That is why the variable names
// should exactly match the names inside the VSS file and be of a string type.
internal class VssNodeSpecModel(
    override var uuid: String = "",
    override var vssPath: String = "",
    override var description: String = "",
    override var type: String = "",
    override var comment: String = "",
    @Suppress("MemberVisibilityCanBePrivate") var datatype: String = "",
) : VssNode, SpecModel<VssNodeSpecModel> {
    var logger: KSPLogger? = null

    private val stringTypeName = String::class.asTypeName()
    private val vssNodeSetTypeName = Set::class.parameterizedBy(VssNode::class)
    private val genericClassTypeName = KClass::class.asClassName().parameterizedBy(STAR)
    private val genericClassTypeNameNullable = KClass::class.asClassName().parameterizedBy(STAR).copy(nullable = true)

    @OptIn(ExperimentalUnsignedTypes::class)
    private val datatypeTypeName: TypeName
        get() {
            return when (datatype) {
                "string" -> String::class.asTypeName()
                "boolean" -> Boolean::class.asTypeName()
                "uint8", "uint16", "uint32" -> UInt::class.asTypeName()
                "uint64" -> ULong::class.asTypeName()
                "int8", "int16", "int32" -> Int::class.asTypeName()
                "int64" -> Long::class.asTypeName()
                "float" -> Float::class.asTypeName()
                "double" -> Double::class.asTypeName()
                "string[]" -> Array::class.parameterizedBy(String::class)
                "boolean[]" -> BooleanArray::class.asTypeName()
                "int8[]", "int16[]", "int32[]" -> IntArray::class.asTypeName()
                "uint8[]", "uint16[]", "uint32[]" -> UIntArray::class.asTypeName()
                "int64[]" -> LongArray::class.asTypeName()
                "uint64[]" -> ULongArray::class.asTypeName()
                "float[]" -> FloatArray::class.asTypeName()
                else -> Any::class.asTypeName()
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    private val valueTypeName: TypeName
        get() {
            return when (datatypeTypeName) {
                // Convert the following Kotlin types because they are incompatible with the @JvmOverloads annotation
                UInt::class.asTypeName() -> Int::class.asTypeName()
                ULong::class.asTypeName() -> Long::class.asTypeName()
                UIntArray::class.asTypeName() -> IntArray::class.asTypeName()
                ULongArray::class.asTypeName() -> LongArray::class.asTypeName()
                else -> datatypeTypeName
            }
        }

    /**
     * Returns valid default values as string literals.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    private val defaultValue: String
        get() {
            return when (valueTypeName) {
                String::class.asTypeName() -> "\"\""
                Boolean::class.asTypeName() -> "false"
                Float::class.asTypeName() -> "0f"
                Double::class.asTypeName() -> "0.0"
                Int::class.asTypeName() -> "0"
                Long::class.asTypeName() -> "0L"
                UInt::class.asTypeName() -> "0u"
                Array::class.parameterizedBy(String::class) -> "emptyArray<String>()"
                IntArray::class.asTypeName() -> "IntArray(0)"
                BooleanArray::class.asTypeName() -> "BooleanArray(0)"
                FloatArray::class.asTypeName() -> "FloatArray(0)"
                LongArray::class.asTypeName() -> "LongArray(0)"
                ULongArray::class.asTypeName() -> "ULongArray(0)"
                UIntArray::class.asTypeName() -> "UIntArray(0)"

                else -> throw IllegalArgumentException("No default value found for $valueTypeName!")
            }
        }

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
            val (vssSignalTypeName, vssSignalPropertySpecs, vssSignalParameterSpec) = createVssSignalSpec()

            // Final leafs should ONLY implement the VssSignal interface
            superInterfaces.clear()
            superInterfaces.add(vssSignalTypeName)

            propertySpecs.addAll(vssSignalPropertySpecs)
            vssSignalParameterSpec?.let { constructorBuilder.addParameter(it) }
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

    private fun createVssSignalSpec(): Triple<ParameterizedTypeName, MutableList<PropertySpec>, ParameterSpec?> {
        val propertySpecs = mutableListOf<PropertySpec>()
        var parameterSpec: ParameterSpec? = null

        val vssSignalMembers = VssSignal::class.declaredMemberProperties
        vssSignalMembers.forEach { member ->
            val memberName = member.name
            when (val memberType = member.returnType.asTypeName()) {
                genericClassTypeName -> {
                    val genericClassSpec = createGenericClassSpec(
                        memberName,
                        memberType,
                        datatypeTypeName.toString(),
                    )
                    propertySpecs.add(genericClassSpec)
                }

                else -> {
                    val (classPropertySpec, classParameterSpec) = createClassParamSpec(
                        memberName,
                        valueTypeName,
                        defaultValue,
                    )
                    parameterSpec = classParameterSpec
                    propertySpecs.add(classPropertySpec)
                }
            }
        }

        val typeName = VssSignal::class
            .asTypeName()
            .plusParameter(valueTypeName)

        return Triple(typeName, propertySpecs, parameterSpec)
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
            propertySpec.name,
            propertySpec.type,
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
