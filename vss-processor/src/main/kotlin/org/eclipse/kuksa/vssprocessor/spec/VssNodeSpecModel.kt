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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
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
    private val genericClassTypeName = KClass::class.asClassName().parameterizedBy(STAR).copy(nullable = true)

    private val datatypeProperty: TypeName
        get() {
            return when (datatype) {
                "string" -> String::class.asTypeName()
                "boolean" -> Boolean::class.asTypeName()
                // Do not use UInt because it is incompatible with @JvmOverloads annotation
                "uint8", "uint16", "uint32" -> Int::class.asTypeName()
                "int8", "int16", "int32" -> Int::class.asTypeName()
                "int64", "uint64" -> Long::class.asTypeName()
                "float" -> Float::class.asTypeName()
                "double" -> Double::class.asTypeName()
                "string[]" -> Array::class.parameterizedBy(String::class)
                "boolean[]" -> BooleanArray::class.asTypeName()
                "uint8[]", "uint16[]", "uint32[]", "int8[]", "int16[]", "int32[]" -> IntArray::class.asTypeName()
                "int64[]", "uint64[]" -> LongArray::class.asTypeName()
                else -> Any::class.asTypeName()
            }
        }

    /**
     * Returns valid default values as string literals.
     */
    private val defaultValue: String
        get() {
            return when (datatypeProperty) {
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
                LongArray::class.asTypeName() -> "LongArray(0)"

                else -> throw IllegalArgumentException("No default value found for $datatypeProperty!")
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
        if (childNodes.isEmpty()) {
            val (valuePropertySpec, parameterSpec) = createValueSpec()

            constructorBuilder.addParameter(parameterSpec)
            propertySpecs.add(valuePropertySpec)

            // Final leafs should ONLY implement the VssSignal interface
            superInterfaces.clear()
            val vssSignalInterface = VssSignal::class
                .asTypeName()
                .plusParameter(datatypeProperty)
            superInterfaces.add(vssSignalInterface)
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
                val defaultParameter = createDefaultParameterSpec(
                    mainClassPropertySpec.name,
                    defaultClassName,
                    uniquePackageName,
                )

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

    private fun createValueSpec(): Pair<PropertySpec, ParameterSpec> {
        val valuePropertySpec = PropertySpec
            .builder(PROPERTY_VALUE_NAME, datatypeProperty)
            .initializer(PROPERTY_VALUE_NAME)
            .addModifiers(KModifier.OVERRIDE)
            .build()

        // Adds a default value (mainly 0 or an empty string)
        val parameterSpec = ParameterSpec.builder(
            valuePropertySpec.name,
            valuePropertySpec.type,
        ).defaultValue("%L", defaultValue).build()

        return Pair(valuePropertySpec, parameterSpec)
    }

    private fun createDefaultParameterSpec(
        parameterName: String,
        defaultClassName: String,
        packageName: String,
    ) = ParameterSpec
        .builder(parameterName, ClassName(packageName, defaultClassName))
        .defaultValue("%L()", defaultClassName)
        .build()

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
        fun createSetSpec(memberName: String, memberType: TypeName): PropertySpec {
            val vssNodeNamesJoined = childNodes.joinToString(", ") { it.variableName }

            return PropertySpec
                .builder(memberName, memberType)
                .mutable(false)
                .addModifiers(KModifier.OVERRIDE)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return setOf(%L)", vssNodeNamesJoined)
                        .build(),
                )
                .build()
        }

        fun createParentSpec(memberName: String, memberType: TypeName): PropertySpec {
            val parentClass = if (parentClassName.isNotEmpty()) "$parentClassName::class" else "null"
            return PropertySpec
                .builder(memberName, memberType)
                .mutable(false)
                .addModifiers(KModifier.OVERRIDE)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return %L", parentClass)
                        .build(),
                )
                .build()
        }

        val propertySpecs = mutableListOf<PropertySpec>()

        val members = VssNode::class.declaredMemberProperties
        members.forEach { member ->
            val memberName = member.name
            when (val memberType = member.returnType.asTypeName()) {
                vssNodeSetTypeName -> createSetSpec(memberName, memberType)
                genericClassTypeName -> createParentSpec(memberName, memberType)
                else -> null
            }?.let { propertySpec ->
                propertySpecs.add(propertySpec)
            }
        }

        return propertySpecs
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
        private const val PROPERTY_VALUE_NAME = "value"
    }
}

internal data class VssPath(val path: String) {
    val leaf: String
        get() = path.substringAfterLast(".")
}
