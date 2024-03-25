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
 */

package org.eclipse.kuksa.extension.vss

import org.eclipse.kuksa.extension.copy
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.proto.v1.Types.Datapoint.ValueCase.*
import org.eclipse.kuksa.vsscore.model.VssNode
import org.eclipse.kuksa.vsscore.model.VssSignal
import org.eclipse.kuksa.vsscore.model.findHeritageLine
import org.eclipse.kuksa.vsscore.model.heritage
import org.eclipse.kuksa.vsscore.model.variableName
import kotlin.reflect.full.declaredMemberProperties

/**
 * Creates a copy of the [VssNode] where the whole [VssNode.findHeritageLine] is replaced
 * with modified heirs.
 *
 * ### Example:
 * ```
 * VssVehicle->VssCabin->VssWindowChildLockEngaged
 * ```
 *
 * A deep copy is necessary for a nested history tree with at least two generations. The VssWindowChildLockEngaged
 * is replaced inside VssCabin where this again is replaced inside VssVehicle. Use the [generation] to start copying
 * from the [VssNode] to the [deepCopy]. Returns a copy where every heir in the given [changedHeritage] is
 * replaced with another copy.
 *
 * @throws [IllegalArgumentException] if the copied types do not match. This can happen if the [heritage] is not
 * correct.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 */
// The suggested method to improve the performance can't be used here because we are already working with a full array.
// https://detekt.dev/docs/rules/performance/
fun <T : VssNode> T.deepCopy(generation: Int = 0, changedHeritage: List<VssNode>): T {
    if (generation == changedHeritage.size) { // Reached the end, use the changed VssSignal
        return this
    }

    // Create the missing link between this [VssNode] and the given node inbetween
    var heritageLine = changedHeritage
    if (changedHeritage.size == 1) {
        heritageLine = findHeritageLine(changedHeritage.first(), true)
            .toList()
            .ifEmpty { changedHeritage }
    }

    val childNode = heritageLine[generation]
    val childCopy = childNode.deepCopy(generation + 1, heritageLine)
    val parameterNameToChild = mapOf(childNode.variableName to childCopy)

    return copy(parameterNameToChild)
}

/**
 * Convenience method for [deepCopy] with a [VssNode]. It will return the [VssNode] with the updated
 * [VssNode].
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 */
fun <T : VssNode> T.deepCopy(vararg vssNodes: VssNode): T {
    return deepCopy(0, vssNodes.toList())
}

/**
 * Creates a copy of a [VssSignal] where the [VssSignal.value] is changed to the given [Datapoint].
 */
// The actual value type is unknown but it is expected that the casted [valueCase] is valid if no exception was thrown.
@Suppress("UNCHECKED_CAST")
fun <T : Any> VssSignal<T>.copy(datapoint: Datapoint): VssSignal<T> {
    with(datapoint) {
        val value: Any = when (valueCase) {
            STRING -> string
            BOOL -> bool
            INT32 -> int32
            INT64 -> int64
            UINT32 -> uint32
            UINT64 -> uint64
            FLOAT -> float
            DOUBLE -> double
            STRING_ARRAY -> stringArray.valuesList.toTypedArray()
            BOOL_ARRAY -> boolArray.valuesList.toBooleanArray()
            INT32_ARRAY -> int32Array.valuesList.toIntArray()
            INT64_ARRAY -> int64Array.valuesList.toLongArray()
            UINT32_ARRAY -> uint32Array.valuesList.toIntArray()
            UINT64_ARRAY -> uint64Array.valuesList.toLongArray()
            FLOAT_ARRAY -> floatArray.valuesList.toFloatArray()
            DOUBLE_ARRAY -> doubleArray.valuesList.toDoubleArray()

            // The server does not know the value type because it was never set yet
            // but we know the expected type. Less types are not a problem here because they will default to 0 for
            // uint, int, double and so on.
            VALUE_NOT_SET -> {
                when (value::class) {
                    String::class -> string
                    Boolean::class -> bool
                    Float::class -> float
                    Double::class -> double
                    Int::class -> int32
                    Long::class -> int64
                    UInt::class -> uint32.toUInt()
                    Array<String>::class -> stringArray.valuesList.toList().toTypedArray()
                    IntArray::class -> int32Array.valuesList.toIntArray()
                    Types.BoolArray::class -> boolArray.valuesList.toBooleanArray()
                    else -> throw NoSuchFieldException("Could not convert value: $value to type: ${value::class}")
                }
            }

            null -> throw NoSuchFieldException("Could not convert available value: $value to type: ${value::class}")
        }

        // Value must be T
        return this@copy.copy(value as T)
    }
}

/**
 * Calls the generated copy method of the data class for the [VssSignal] and returns a new copy with the new [value].
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method nor [valuePropertyName] was found for the class.
 */
@JvmOverloads
fun <T : Any> VssSignal<T>.copy(value: T, valuePropertyName: String = "value"): VssSignal<T> {
    val memberProperties = VssSignal::class.declaredMemberProperties
    val firstPropertyName = memberProperties.first { it.name == valuePropertyName }.name
    val valueMap = mapOf(firstPropertyName to value)

    return this@copy.copy(valueMap)
}

/**
 * Creates a copy of the [VssNode] where the heir with a matching [vssPath] is replaced with the
 * [updatedValue].
 *
 * @param consideredHeritage the heritage of the [VssNode] which is considered for searching. The default
 * will always generate the up to date heritage of the current [VssNode]. For performance reason it may make
 * sense to cache the input and reuse the [Collection] here.
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 */
@Suppress("UNCHECKED_CAST")
fun <T : VssNode> T.copy(
    vssPath: String,
    updatedValue: Datapoint,
    consideredHeritage: Collection<VssNode> = heritage,
): T {
    val vssNodes = consideredHeritage + this
    val vssNode = vssNodes
        .filterIsInstance<VssSignal<*>>()
        .find { it.vssPath == vssPath } ?: return this

    val updatedVssNode = vssNode.copy(updatedValue)

    // Same node with no heirs, no deep copy is needed
    if (this.vssPath == updatedVssNode.vssPath) return updatedVssNode as T

    return deepCopy(updatedVssNode)
}

// region Operators

/**
 * Convenience operator for [deepCopy] with a [VssNode]. It will return the parent [VssNode] with the updated child
 * [VssNode].
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 */
operator fun <T : VssNode> T.invoke(vararg vssNodes: VssNode): T {
    return deepCopy(0, vssNodes.toList())
}

/**
 * Convenience operator for [copy] with a value [T].
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 */
operator fun <T : Any> VssSignal<T>.invoke(value: T): VssSignal<T> {
    return copy(value)
}

// endregion
