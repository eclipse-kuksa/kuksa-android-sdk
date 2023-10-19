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
 * ### Example:
 * ```
 * VssVehicle->VssCabin->VssWindowChildLockEngaged
 * ```
 *
 * A deep copy is necessary for a nested history tree with at least two generations. The VssWindowChildLockEngaged
 * is replaced inside VssCabin where this again is replaced inside VssVehicle. Use the [generation] to start copying
 * from the [VssSpecification] to the [deepCopy]. Returns a copy where every heir in the given [changedHeritage] is
 * replaced with a another copy
 */
@Suppress("performance:SpreadOperator")
fun <T : VssSpecification> T.deepCopy(generation: Int = 0, vararg changedHeritage: VssSpecification): T {
    if (generation == changedHeritage.size) { // Reached the end, use the changed VssProperty
        return this
    }

    // Create the missing link between this (VssSpecification) and the given property (VssSpecifications inbetween)
    var heritageLine = changedHeritage
    if (changedHeritage.size == 1) {
        heritageLine = findHeritageLine(changedHeritage.first(), true)
            .toTypedArray()
            .ifEmpty { changedHeritage }
    }

    val childSpecification = heritageLine[generation]
    val childCopy = childSpecification.deepCopy(generation + 1, *heritageLine)
    val parameterNameToChild = mapOf(childSpecification.variableName to childCopy)

    return copy(parameterNameToChild)
}

/**
 * Creates a copy of a [VssProperty] where the [VssProperty.value] is changed to the given [Datapoint].
 *
 * @throws [NoSuchElementException] if the class has no "copy" method
 * @throws [IllegalArgumentException] if the copied types do not match
 */
@Suppress("UNCHECKED_CAST")
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
 * Calls the generated copy method of the data class for the [VssProperty] and returns a new copy with the new [value].
 */
fun <T : Any> VssProperty<T>.copy(value: T): VssProperty<T> {
    val valueMap = mapOf("value" to value)
    return this@copy.copy(valueMap)
}

/**
 * Creates a copy of the [VssSpecification] where the heir with a matching [vssPath] is replaced with the
 * [updatedValue].
 *
 * @param consideredHeritage the heritage of the [VssSpecification] which is considered for searching. The default
 * will always generate the up to date heritage of the current [VssSpecification]. For performance reason it may make
 * sense to cache the input and reuse the [Collection] here.
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

    // Same property with no heirs, no deep copy is needed
    if (this.vssPath == updatedVssProperty.vssPath) return updatedVssProperty as T

    return deepCopy(0, updatedVssProperty)
}

/**
 * Convenience operator for [copy] with a value [T].
 */
operator fun <T : Any> VssProperty<T>.plusAssign(value: T) {
    copy(value)
}

/**
 * Convenience operator for [copy] with a value [T].
 */
operator fun <T : Any> VssProperty<T>.plus(value: T): VssProperty<T> {
    return copy(value)
}

/**
 * Convenience operator for [copy] with a [Boolean] value which will be inverted.
 */
operator fun VssProperty<Boolean>.not(): VssProperty<Boolean> {
    return copy(!value)
}

/**
 * Convenience operator for [deepCopy] with a [VssProperty]. It will return the [VssSpecification] with the updated
 * [VssProperty].
 */
operator fun <T : VssSpecification, V : Any> T.plus(property: VssProperty<V>): T {
    return deepCopy(0, property)
}
