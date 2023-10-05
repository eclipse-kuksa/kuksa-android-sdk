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

package org.eclipse.kuksa.extension

import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.proto.v1.Types.Datapoint.ValueCase.*
import org.eclipse.kuksa.vsscore.model.VssProperty
import org.eclipse.kuksa.vsscore.model.VssSpecification
import org.eclipse.kuksa.vsscore.model.findHeritageLine
import org.eclipse.kuksa.vsscore.model.heritage
import org.eclipse.kuksa.vsscore.model.variableName

/**
 * Creates a copy of the [VssSpecification] where the whole [VssSpecification.findHeritageLine] is replaced
 * with modified heirs.
 *
 * Example: VssVehicle->VssCabin->VssWindowChildLockEngaged
 * A deep copy is necessary for a nested history tree with at least two generations. The VssWindowChildLockEngaged
 * is replaced inside VssCabin where this again is replaced inside VssVehicle.
 *
 * @param changedHeritageLine the line of heirs
 * @param generation the generation to start copying with starting from the [VssSpecification] to [deepCopy]
 * @return a copy where every heir in the given [changedHeritageLine] is replaced with a another copy
 */
fun <T : VssSpecification> T.deepCopy(changedHeritageLine: List<VssSpecification>, generation: Int = 0): T {
    if (generation == changedHeritageLine.size) { // Reached the end, use the changed VssProperty
        return this
    }

    val childSpecification = changedHeritageLine[generation]
    val childCopy = childSpecification.deepCopy(changedHeritageLine, generation + 1)
    val parameterNameToChild = mapOf(childSpecification.variableName to childCopy)

    return copy(parameterNameToChild)
}

/**
 * Creates a copy of a [VssProperty] where the [VssProperty.value] is changed to the given [Datapoint].
 *
 * @param datapoint the [Datapoint.value_] is converted to the correct datatype depending on the [VssProperty.value]
 * @return a copy of the [VssProperty] with the updated [VssProperty.value]
 *
 * @throws [NoSuchElementException] if the class has no "copy" method
 * @throws [IllegalArgumentException] if the copied types do not match
 */
fun <T : Any> VssProperty<T>.copy(datapoint: Datapoint): VssProperty<T> {
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
            STRING_ARRAY -> stringArray.valuesList
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
                    Array::class -> stringArray.valuesList.toList().toTypedArray()
                    IntArray::class -> int32Array.valuesList.toIntArray()
                    Types.BoolArray::class -> boolArray.valuesList.toBooleanArray()
                    else -> throw NoSuchFieldException("Could not convert value: $value to type: ${value::class}")
                }
            }

            null -> throw NoSuchFieldException("Could not convert value: $value to type: ${value::class}")
        }

        val valueMap = mapOf("value" to value)
        return this@copy.copy(valueMap)
    }
}

/**
 * Creates a copy of the [VssSpecification] where the heir with a matching [vssPath] is replaced with the
 * [updatedValue].
 *
 * @param vssPath which is used to find the correct heir in the [VssSpecification]
 * @param updatedValue which will be updated inside the matching [VssProperty]
 * @param consideredHeritage the heritage of the [VssSpecification] which is considered for searching. The default
 * will always generate the up to date heritage of the current [VssSpecification]. For performance reason it may make
 * sense to cache the input and reuse the [Collection] here.
 * @return a copy where the heir with the matching [vssPath] is replaced with a another copy
 *
 * @throws [NoSuchElementException] if the class has no "copy" method
 * @throws [IllegalArgumentException] if the copied types do not match
 */
@Suppress("UNCHECKED_CAST")
fun <T : VssSpecification> T.copy(
    vssPath: String,
    updatedValue: Datapoint,
    consideredHeritage: Collection<VssSpecification> = heritage,
): T {
    val vssSpecifications = consideredHeritage + this
    val vssProperty = vssSpecifications
        .filterIsInstance<VssProperty<*>>()
        .find { it.vssPath == vssPath } ?: return this

    val updatedVssProperty = vssProperty.copy(updatedValue)
    val relevantChildren = findHeritageLine(vssProperty).toMutableList()

    // Replace the last specification (Property) with the changed one
    val updatedVssSpecification: T = if (relevantChildren.isNotEmpty()) {
        relevantChildren.removeLast()
        relevantChildren.add(updatedVssProperty)

        this.deepCopy(relevantChildren)
    } else {
        updatedVssProperty as T // The property must be T since no children are available
    }

    return updatedVssSpecification
}
