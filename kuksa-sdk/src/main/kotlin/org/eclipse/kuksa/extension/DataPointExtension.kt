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

import android.util.Log
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.proto.v1.Types.Datapoint.ValueCase
import org.eclipse.kuksa.vsscore.model.VssSignal

private const val CSV_DELIMITER = ","

/**
 * Returns the converted VSS value types -> Protobuf data types.
 */
val Types.Metadata.valueType: ValueCase
    get() = dataType.dataPointValueCase

/**
 * Converts the [VssSignal.value] into a [Datapoint] object. The [VssSignal.dataType] is used to derive the correct
 * [ValueCase].
 *
 * @throws IllegalArgumentException if the [VssSignal] could not be converted to a [Datapoint].
 */
val <T : Any> VssSignal<T>.datapoint: Datapoint
    get() {
        // TODO: Only supports string arrays for now, IntArray, DoubleArray etc. are not supported yet because
        // TODO: IntArrays are custom types which to not implement the Array interface and can't be cast to it.
        val stringValue = if (value::class.java.isArray) {
            val valueArray = value as Array<*>
            valueArray.joinToString()
        } else {
            value.toString()
        }

        return valueCase.createDatapoint(stringValue)
    }

/**
 * Converts the [VssSignal.value] into a [ValueCase] enum. The [VssSignal.dataType] is used to derive the correct
 * [ValueCase].
 *
 * @throws IllegalArgumentException if the [VssSignal] could not be converted to a [ValueCase].
 */
@OptIn(ExperimentalUnsignedTypes::class)
val <T : Any> VssSignal<T>.valueCase: ValueCase
    get() {
        return when (dataType) {
            String::class -> ValueCase.STRING
            Boolean::class -> ValueCase.BOOL
            Int::class -> ValueCase.INT32
            Float::class -> ValueCase.FLOAT
            Double::class -> ValueCase.DOUBLE
            Long::class -> ValueCase.INT64
            UInt::class -> ValueCase.UINT32
            ULong::class -> ValueCase.UINT64
            Array<String>::class -> ValueCase.STRING_ARRAY
            BooleanArray::class -> ValueCase.BOOL_ARRAY
            IntArray::class -> ValueCase.INT32_ARRAY
            FloatArray::class -> ValueCase.FLOAT_ARRAY
            DoubleArray::class -> ValueCase.DOUBLE_ARRAY
            LongArray::class -> ValueCase.INT64_ARRAY
            UIntArray::class -> ValueCase.UINT32_ARRAY
            ULongArray::class -> ValueCase.UINT64_ARRAY

            else -> throw IllegalArgumentException("Could not create value case for value class: ${dataType::class}!")
        }
    }

/**
 * Creates a [Datapoint] object with a given [value] which is in [String] format. The [String] will be converted
 * to the correct type for the [Datapoint.Builder].
 */
fun ValueCase.createDatapoint(value: String): Datapoint {
    val datapointBuilder = Datapoint.newBuilder()

    try {
        when (this) {
            ValueCase.VALUE_NOT_SET, // also explicitly handled on UI level
            ValueCase.STRING,
            -> datapointBuilder.string = value

            ValueCase.UINT32 ->
                datapointBuilder.uint32 = value.toInt()

            ValueCase.INT32 ->
                datapointBuilder.int32 = value.toInt()

            ValueCase.UINT64 ->
                datapointBuilder.uint64 = value.toLong()

            ValueCase.INT64 ->
                datapointBuilder.int64 = value.toLong()

            ValueCase.FLOAT ->
                datapointBuilder.float = value.toFloat()

            ValueCase.DOUBLE ->
                datapointBuilder.double = value.toDouble()

            ValueCase.BOOL ->
                datapointBuilder.bool = value.toBoolean()

            ValueCase.STRING_ARRAY ->
                datapointBuilder.stringArray = createStringArray(value)

            ValueCase.UINT32_ARRAY ->
                datapointBuilder.uint32Array = createUInt32Array(value)

            ValueCase.INT32_ARRAY ->
                datapointBuilder.int32Array = createInt32Array(value)

            ValueCase.UINT64_ARRAY ->
                datapointBuilder.uint64Array = createUInt64Array(value)

            ValueCase.INT64_ARRAY ->
                datapointBuilder.int64Array = createInt64Array(value)

            ValueCase.FLOAT_ARRAY ->
                datapointBuilder.floatArray = createFloatArray(value)

            ValueCase.DOUBLE_ARRAY ->
                datapointBuilder.doubleArray = createDoubleArray(value)

            ValueCase.BOOL_ARRAY ->
                datapointBuilder.boolArray = createBoolArray(value)
        }
    } catch (e: NumberFormatException) {
        Log.w(TAG, "Could not convert value: $value to ValueCase: $this", e)
        datapointBuilder.string = value // Fallback to string
    }

    return datapointBuilder.build()
}

/**
 * Returns the contained value inside the [Datapoint] as a string representation.
 */
val Datapoint.stringValue: String
    get() {
        val value: Any = when (valueCase) {
            ValueCase.STRING -> string
            ValueCase.UINT32 -> uint32
            ValueCase.INT32 -> int32
            ValueCase.UINT64 -> uint64
            ValueCase.INT64 -> int64
            ValueCase.FLOAT -> float
            ValueCase.DOUBLE -> double
            ValueCase.BOOL -> bool
            ValueCase.STRING_ARRAY -> stringArray
            ValueCase.UINT32_ARRAY -> uint32Array
            ValueCase.INT32_ARRAY -> int32Array
            ValueCase.UINT64_ARRAY -> uint64Array
            ValueCase.INT64_ARRAY -> int64Array
            ValueCase.FLOAT_ARRAY -> floatArray
            ValueCase.DOUBLE_ARRAY -> doubleArray
            ValueCase.BOOL_ARRAY -> boolArray
            ValueCase.VALUE_NOT_SET -> ""
            null -> ""
        }

        return value.toString()
    }

private fun createBoolArray(value: String): Types.BoolArray {
    val csvValues = value.split(CSV_DELIMITER).map { it.toBoolean() }

    return Types.BoolArray.newBuilder()
        .addAllValues(csvValues)
        .build()
}

private fun createDoubleArray(value: String): Types.DoubleArray {
    val csvValues = value.split(CSV_DELIMITER).map { it.toDouble() }

    return Types.DoubleArray.newBuilder()
        .addAllValues(csvValues)
        .build()
}

private fun createInt64Array(value: String): Types.Int64Array {
    val csvValues = value.split(CSV_DELIMITER).map { it.toLong() }

    return Types.Int64Array.newBuilder()
        .addAllValues(csvValues)
        .build()
}

private fun createUInt64Array(value: String): Types.Uint64Array {
    val csvValues = value.split(CSV_DELIMITER).map { it.toLong() }

    return Types.Uint64Array.newBuilder()
        .addAllValues(csvValues)
        .build()
}

private fun createInt32Array(value: String): Types.Int32Array {
    val csvValues = value.split(CSV_DELIMITER).map { it.toInt() }

    return Types.Int32Array.newBuilder()
        .addAllValues(csvValues)
        .build()
}

private fun createUInt32Array(value: String): Types.Uint32Array {
    val csvValues = value.split(CSV_DELIMITER).map { it.toInt() }

    return Types.Uint32Array.newBuilder()
        .addAllValues(csvValues)
        .build()
}

private fun createStringArray(value: String): Types.StringArray {
    val csvValues = value.split(CSV_DELIMITER)

    return Types.StringArray.newBuilder()
        .addAllValues(csvValues)
        .build()
}

private fun createFloatArray(value: String): Types.FloatArray {
    val csvValues = value.split(CSV_DELIMITER).map { it.toFloat() }

    return Types.FloatArray.newBuilder()
        .addAllValues(csvValues)
        .build()
}
