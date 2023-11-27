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

package org.eclipse.kuksa.vsscore.model

import org.eclipse.kuksa.vsscore.extension.toCamelCase
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

/**
 * Represents a node inside a VSS specification file.
 */
interface VssNode {
    /**
     * A collection of all initialized children.
     */
    val children: Set<VssSpecification>
        get() = emptySet()

    /**
     * The [KClass] of the parent. Can be used to reconstruct the node tree from a child perspective.
     */
    val parentClass: KClass<*>?
        get() = null
}

/**
 * In addition of being a [VssNode] it represents the most common properties of a VSS specification. The [uuid] is a
 * mandatory field and should never be empty.
 */
interface VssSpecification : VssNode {
    val uuid: String
    val vssPath: String
    val description: String
    val type: String
    val comment: String
}

/**
 * Some [VssSpecification] may have an additional [value] property. These are children which are not parents.
 */
interface VssProperty<T : Any> : VssSpecification {
    val value: T
}

/**
 * Splits the [VssSpecification.vssPath] into its parts.
 */
val VssSpecification.vssPathComponents: List<String>
    get() = vssPath.split(".")

/**
 * Generates a heritage line with the [VssSpecification.vssPath] from the most known parent.
 * E.g. "Vehicle.OBD.Catalyst" -> [Vehicle, Vehicle.OBD, Vehicle.OBD.Catalyst]
 */
val VssSpecification.vssPathHeritageLine: List<String>
    get() {
        val components = vssPathComponents
        return components.foldIndexed(emptyList()) { index, accumulation, _ ->
            val nextComponent = components.subList(0, index + 1)
            accumulation + nextComponent.joinToString(".")
        }
    }

/**
 * Parses a name from the [VssSpecification.vssPath].
 */
val VssSpecification.name: String
    get() = vssPath.substringAfterLast(".")

/**
 * Return the parent [VssSpecification.vssPath].
 */
val VssSpecification.parentVssPath: String
    get() = vssPath.substringBeforeLast(".", "")

/**
 * Returns the parent key depending on the [VssSpecification.vssPath].
 */
val VssSpecification.parentKey: String
    get() {
        val keys = vssPathComponents
        if (keys.size < 2) return ""

        return keys[keys.size - 2]
    }

/**
 * Iterates through all nested children which also may have children and aggregates them into one big collection.
 */
val VssSpecification.heritage: Collection<VssSpecification>
    get() = children.toList() + children.flatMap { it.heritage }

/**
 * Finds the latest generation in the form of [VssProperty] for the current [VssSpecification].
 */
val VssSpecification.vssProperties: Collection<VssProperty<*>>
    get() = heritage
        .ifEmpty { setOf(this) }
        .filterIsInstance<VssProperty<*>>()

/**
 * Uses the [variablePrefix] to generate a unique variable name. The first character is at least lowercased.
 * If the [name] is something like "ABS" then it is converted to "abs" instead of "aBS".
 */
val VssSpecification.variableName: String // Fixes duplicates e.g. type as variable and nested type
    get() {
        val fullName = (variablePrefix + name).toCamelCase

        return fullName.replaceFirstChar { it.lowercase() }
    }

/**
 * Similar to the [variableName] but does not lowercase the [name] wherever necessary.
 */
val VssSpecification.className: String
    get() {
        return (classNamePrefix + name).toCamelCase.replaceFirstChar { it.uppercase() }
    }

/**
 * Used in case of conflicted naming with child properties.
 */
private val VssSpecification.variablePrefix: String
    get() = if (isVariableOccupied) classNamePrefix else ""

/**
 * True if the [name] clashes with a property name.
 */
private val VssSpecification.isVariableOccupied: Boolean
    get() {
        val declaredMemberProperties = VssSpecification::class.declaredMemberProperties
        return declaredMemberProperties.find { member ->
            member.name.equals(name, true)
        } != null
    }

private val classNamePrefix: String
    get() = "Vss"

/**
 * Creates an inheritance line to the given [heir]. Similar to [vssPathHeritageLine] but the other way around. It
 * returns a [Collection] of the full heritage line in the form of [VssSpecification].
 *
 * ### Hint
 * The given heir is only used to find the heir inside the [VssSpecification]. It may differ from the one which is
 * returned. If you want the heritage replaced by the given [heir] parameter then use the [isReplacingHeir] parameter.
 */
fun VssSpecification.findHeritageLine(
    heir: VssSpecification,
    isReplacingHeir: Boolean = false,
): Collection<VssSpecification> {
    val specificationKeys = heir.vssPathHeritageLine
    val heritageLine = heritage.filter { child ->
        specificationKeys.contains(child.vssPath)
    }.toMutableList()

    if (isReplacingHeir && heritageLine.isNotEmpty()) {
        heritageLine.removeLast()
        heritageLine.add(heir)
    }

    return heritageLine
}

/**
 * Finds the given [property] inside the current [VssSpecification].
 */
fun <T : VssProperty<V>, V : Any> VssSpecification.findProperty(property: VssProperty<V>): VssProperty<V> {
    return heritage
        .filterIsInstance<VssProperty<V>>()
        .first { it.uuid == property.uuid }
}

/**
 * Finds all [VssProperty] which matches the given [KClass.simpleName]. This is useful when multiple nested objects
 * with the same Name exists but are pretty much the same besides the [VssSpecification.vssPath] etc.
 */
fun <T : VssProperty<V>, V : Any> VssSpecification.findProperties(type: KClass<T>): Map<String, VssProperty<V>> {
    return heritage
        .filterIsInstance<VssProperty<V>>()
        .filter { it::class.simpleName == type.simpleName }
        .associateBy { it.vssPath }
}
