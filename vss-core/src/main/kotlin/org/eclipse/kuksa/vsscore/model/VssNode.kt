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
 * Represents a node inside a VSS (file) data structure. it represents the most common properties.
 */
interface VssNode {
    /**
     * The [uuid] is a mandatory field and should never be empty.
     */
    val uuid: String

    /**
     * Defines the path to the [VssNode] inside the VSS tree structure.
     * Example: Vehicle.Body.Horn.IsActive
     */
    val vssPath: String

    /**
     * Simple description of the [VssNode].
     */
    val description: String

    /**
     * Most relevant for [VssSignal] nodes. They can be of the type "Sensor" or "Actuator".
     * For Nodes with children this will always be "branch".
     */
    val type: String

    /**
     * An optional additional comment for the [VssNode].
     */
    val comment: String

    /**
     * A collection of all initialized children.
     */
    // Always empty for VssSignals. If this is moved to the VssBranch interface then it introduces some API
    // inconveniences because most search API return a VssNode so a cast is always necessary for further searches.
    val children: Set<VssNode>
        get() = emptySet()

    /**
     * The [KClass] of the parent. Can be used to reconstruct the node tree from a child perspective.
     */
    val parentClass: KClass<*>?
        get() = null
}

/**
 * Splits the [VssNode.vssPath] into its parts.
 */
val VssNode.vssPathComponents: List<String>
    get() = vssPath.split(".")

/**
 * Generates a heritage line with the [VssNode.vssPath] from the most known parent.
 * E.g. "Vehicle.OBD.Catalyst" -> [Vehicle, Vehicle.OBD, Vehicle.OBD.Catalyst]
 */
val VssNode.vssPathHeritageLine: List<String>
    get() {
        val components = vssPathComponents
        return components.foldIndexed(emptyList()) { index, accumulation, _ ->
            val nextComponent = components.subList(0, index + 1)
            accumulation + nextComponent.joinToString(".")
        }
    }

/**
 * Parses a name from the [VssNode.vssPath].
 */
val VssNode.name: String
    get() = vssPath.substringAfterLast(".")

/**
 * Return the parent [VssNode.vssPath].
 */
val VssNode.parentVssPath: String
    get() = vssPath.substringBeforeLast(".", "")

/**
 * Returns the parent key depending on the [VssNode.vssPath].
 */
val VssNode.parentKey: String
    get() {
        val keys = vssPathComponents
        if (keys.size < 2) return ""

        return keys[keys.size - 2]
    }

/**
 * Similar to the [variableName] but for the parent and does not lowercase the [name] wherever necessary.
 */
val VssNode.parentClassName: String
    get() {
        if (parentKey.isEmpty()) return ""

        return (classNamePrefix + parentKey).toCamelCase.replaceFirstChar { it.uppercase() }
    }

/**
 * Iterates through all nested children of the [VssNode] which also may have children and aggregates them into one
 * big collection.
 */
val VssNode.heritage: Collection<VssNode>
    get() = children.toList() + children.flatMap { it.heritage }

/**
 * Finds the latest generation in the form of [VssSignal] for the current [VssNode].
 */
val VssNode.vssSignals: Collection<VssSignal<*>>
    get() = heritage
        .ifEmpty { setOf(this) }
        .filterIsInstance<VssSignal<*>>()

/**
 * Uses the [variablePrefix] to generate a unique variable name. The first character is at least lowercased.
 * If the [name] is something like "ABS" then it is converted to "abs" instead of "aBS".
 */
val VssNode.variableName: String // Fixes duplicates e.g. type as variable and nested type
    get() {
        val fullName = (variablePrefix + name).toCamelCase

        return fullName.replaceFirstChar { it.lowercase() }
    }

/**
 * Similar to the [variableName] but does not lowercase the [name] wherever necessary.
 */
val VssNode.className: String
    get() {
        return (classNamePrefix + name).toCamelCase.replaceFirstChar { it.uppercase() }
    }

/**
 * Used in case of conflicted naming with child properties.
 */
private val VssNode.variablePrefix: String
    get() = if (isVariableOccupied) classNamePrefix else ""

/**
 * True if the [name] clashes with a property name.
 */
private val VssNode.isVariableOccupied: Boolean
    get() {
        val declaredMemberProperties = VssNode::class.declaredMemberProperties
        return declaredMemberProperties.find { member ->
            member.name.equals(name, true)
        } != null
    }

private val classNamePrefix: String
    get() = "Vss"

/**
 * Creates an inheritance line to the given [heir]. Similar to [vssPathHeritageLine] but the other way around. It
 * returns a [Collection] of the full heritage line in the form of [VssNode].
 *
 * ### Hint
 * The given heir is only used to find the heir inside the [VssNode]. It may differ from the one which is
 * returned. If you want the heritage replaced by the given [heir] parameter then use the [isReplacingHeir] parameter.
 */
fun VssNode.findHeritageLine(
    heir: VssNode,
    isReplacingHeir: Boolean = false,
): Collection<VssNode> {
    val vssNodeKeys = heir.vssPathHeritageLine
    val heritageLine = heritage.filter { child ->
        vssNodeKeys.contains(child.vssPath)
    }.toMutableList()

    if (isReplacingHeir && heritageLine.isNotEmpty()) {
        heritageLine.removeLast()
        heritageLine.add(heir)
    }

    return heritageLine
}
