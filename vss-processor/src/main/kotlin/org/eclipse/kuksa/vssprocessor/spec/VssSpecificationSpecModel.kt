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
import org.eclipse.kuksa.vsscore.model.VssNode
import org.eclipse.kuksa.vsscore.model.VssProperty
import org.eclipse.kuksa.vsscore.model.VssSpecification
import org.eclipse.kuksa.vsscore.model.className
import org.eclipse.kuksa.vsscore.model.name
import org.eclipse.kuksa.vsscore.model.variableName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

// Reflects the specification file as a model and is filled via reflection. That is why the variable names
// should exactly match the names inside the specification file and be of a string type.
internal class VssSpecificationSpecModel(
    override var uuid: String = "",
    override var vssPath: String = "",
    override var description: String = "",
    override var type: String = "",
    override var comment: String = "",
    @Suppress("MemberVisibilityCanBePrivate") var datatype: String = "",
) : VssSpecification, SpecModel {
    var childSpecifications = mutableListOf<VssSpecificationSpecModel>()

    private lateinit var logger: KSPLogger
    private lateinit var packageName: String

    private val datatypeProperty: TypeName
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
    private val defaultValue: String?
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

                else -> null
            }
        }

    override fun createClassSpec(nestedClasses: Set<String>, packageName: String, logger: KSPLogger): TypeSpec {
        this.packageName = packageName
        this.logger = logger

        val nestedChildSpecs = mutableListOf<TypeSpec>()
        val constructorBuilder = FunSpec.constructorBuilder()
        val propertySpecs = mutableListOf<PropertySpec>()
        val superInterfaces = mutableSetOf<TypeName>(VssSpecification::class.asTypeName())

        // The last element in the chain should have a value like "isLocked".
        if (childSpecifications.isEmpty()) {
            val (valuePropertySpec, parameterSpec) = createValueSpec()

            constructorBuilder.addParameter(parameterSpec)
            propertySpecs.add(valuePropertySpec)

            // Final leafs should ONLY implement the vss property interface
            superInterfaces.clear()
            val vssPropertyInterface = VssProperty::class.asTypeName()
                .plusParameter(datatypeProperty)
            superInterfaces.add(vssPropertyInterface)
        }

        val propertySpec = createVssSpecificationSpecs(className, packageName = packageName)
        propertySpecs.addAll(propertySpec)

        // Parses all specifications into properties
        childSpecifications.forEach { childSpecification ->
            // This nested specification has an ambiguous name, add as nested class
            // The package name is different for nested classes (no qualifier)
            val hasAmbiguousName = nestedClasses.contains(childSpecification.name)
            val uniquePackageName = if (hasAmbiguousName) "" else packageName

            val childPropertySpec = createVssSpecificationSpecs(className, uniquePackageName, childSpecification)
            propertySpecs.addAll(childPropertySpec)

            // Nested VssSpecification properties should be added as constructor parameters
            val mainClassPropertySpec = childPropertySpec.first()
            if (mainClassPropertySpec.initializer != null) { // Only add a default for initializer
                if (hasAmbiguousName) {
                    val childSpec = childSpecification.createClassSpec(nestedClasses, packageName, logger)
                    nestedChildSpecs.add(childSpec)
                }

                val defaultClassName = childSpecification.className
                val defaultParameter = createDefaultParameterSpec(
                    mainClassPropertySpec.name,
                    defaultClassName,
                    uniquePackageName,
                )

                constructorBuilder.addParameter(defaultParameter)
            }
        }

        val nodeSpecs = createVssNodeSpecs(childSpecifications)
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

    private fun createVssSpecificationSpecs(
        className: String,
        packageName: String,
        specification: VssSpecificationSpecModel = this,
    ): List<PropertySpec> {
        val propertySpecs = mutableListOf<PropertySpec>()
        val members = VssSpecification::class.declaredMemberProperties

        fun createInterfaceDataTypeSpec(member: KProperty1<VssSpecification, *>): PropertySpec {
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
            specification: VssSpecificationSpecModel,
            packageName: String,
        ): PropertySpec {
            val prefixedTypeName = ClassName(packageName, specification.className)
            val variableName = specification.variableName

            return PropertySpec
                .builder(variableName, prefixedTypeName)
                .initializer(variableName)
                .build()
        }

        // Add primitive data types
        if (specification.className == className) {
            members.forEach { member ->
                val primitiveDataTypeSpec = createInterfaceDataTypeSpec(member)
                propertySpecs.add(primitiveDataTypeSpec)
            }

            return propertySpecs
        }

        // Add nested child classes
        val objectTypeSpec = createObjectTypeSpec(specification, packageName)
        return listOf(objectTypeSpec)
    }

    private fun createVssNodeSpecs(childSpecifications: List<VssSpecificationSpecModel>): List<PropertySpec> {
        fun createSetSpec(memberName: String, memberType: TypeName): PropertySpec {
            val specificationNamesJoined = childSpecifications.joinToString(", ") { it.variableName }

            return PropertySpec
                .builder(memberName, memberType)
                .mutable(false)
                .addModifiers(KModifier.OVERRIDE)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return setOf(%L)", specificationNamesJoined)
                        .build(),
                )
                .build()
        }

        fun createParentSpec(memberName: String, memberType: TypeName): PropertySpec {
            return PropertySpec
                .builder(memberName, memberType)
                .mutable(false)
                .addModifiers(KModifier.OVERRIDE)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return %L", "$className::class")
                        .build(),
                )
                .build()
        }

        val propertySpecs = mutableListOf<PropertySpec>()

        val members = VssNode::class.declaredMemberProperties
        members.forEach { member ->
            val memberName = member.name
            val memberType = member.returnType.asTypeName()

            val setTypeName = Set::class.parameterizedBy(VssSpecification::class)
            val classTypeName = KClass::class.asClassName().parameterizedBy(STAR).copy(nullable = true)
            val propertySpec: PropertySpec? = when (memberType) {
                setTypeName -> createSetSpec(memberName, memberType)
                classTypeName -> createParentSpec(memberName, memberType)
                else -> null
            }

            propertySpec?.let { propertySpecs.add(it) }
        }

        return propertySpecs
    }

    override fun equals(other: Any?): Boolean {
        if (other !is VssSpecificationSpecModel) return false

        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    companion object {
        private const val PROPERTY_VALUE_NAME = "value"
    }
}

internal data class VssPath(val path: String) {
    val leaf: String
        get() = path.substringAfterLast(".")
}
