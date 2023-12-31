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
import org.eclipse.kuksa.proto.v1.Types.BoolArray
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.proto.v1.Types.Datapoint.ValueCase
import org.eclipse.kuksa.vsscore.model.VssProperty

private const val CSV_DELIMITER = ","

/**
 * Returns the converted VSS value types -> Protobuf data types.
 */
val Types.Metadata.valueType: ValueCase
    get() = dataType.dataPointValueCase

/**
 * Converts the [VssProperty.value] into a [Datapoint] object.
 *
 * @throws IllegalArgumentException if the [VssProperty] could not be converted to a [Datapoint].
 */
val <T : Any> VssProperty<T>.datapoint: Datapoint
    get() {
        val stringValue = value.toString()
        return when (value::class) {
            String::class -> ValueCase.STRING.createDatapoint(stringValue)
            Boolean::class -> ValueCase.BOOL.createDatapoint(stringValue)
            Float::class -> ValueCase.FLOAT.createDatapoint(stringValue)
            Double::class -> ValueCase.DOUBLE.createDatapoint(stringValue)
            Int::class -> ValueCase.INT32.createDatapoint(stringValue)
            Long::class -> ValueCase.INT64.createDatapoint(stringValue)
            UInt::class -> ValueCase.UINT32.createDatapoint(stringValue)
            Array<String>::class -> ValueCase.DOUBLE.createDatapoint(stringValue)
            IntArray::class -> ValueCase.INT32_ARRAY.createDatapoint(stringValue)
            BooleanArray::class -> ValueCase.BOOL_ARRAY.createDatapoint(stringValue)
            LongArray::class -> ValueCase.INT64_ARRAY.createDatapoint(stringValue)

            else -> throw IllegalArgumentException("Could not create datapoint for the value class: ${value::class}!")
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
        Log.w(TAG, "Could not convert value: $value to ValueCase: $this")
        datapointBuilder.string = value // Fallback to string
    }

    return datapointBuilder.build()
}

private fun createBoolArray(value: String): BoolArray {
    val csvValues = value.split(CSV_DELIMITER).map { it.toBoolean() }

    val array = BoolArray.getDefaultInstance()
    array.valuesList.addAll(csvValues)

    return array
}

private fun createDoubleArray(value: String): Types.DoubleArray {
    val csvValues = value.split(CSV_DELIMITER).map { it.toDouble() }

    val array = Types.DoubleArray.getDefaultInstance()
    array.valuesList.addAll(csvValues)

    return array
}

private fun createInt64Array(value: String): Types.Int64Array {
    val csvValues = value.split(CSV_DELIMITER).map { it.toLong() }

    val array = Types.Int64Array.getDefaultInstance()
    array.valuesList.addAll(csvValues)

    return array
}

private fun createUInt64Array(value: String): Types.Uint64Array {
    val csvValues = value.split(CSV_DELIMITER).map { it.toLong() }

    val array = Types.Uint64Array.getDefaultInstance()
    array.valuesList.addAll(csvValues)

    return array
}

private fun createInt32Array(value: String): Types.Int32Array {
    val csvValues = value.split(CSV_DELIMITER).map { it.toInt() }

    val array = Types.Int32Array.getDefaultInstance()
    array.valuesList.addAll(csvValues)

    return array
}

private fun createUInt32Array(value: String): Types.Uint32Array {
    val csvValues = value.split(CSV_DELIMITER).map { it.toInt() }

    val array = Types.Uint32Array.getDefaultInstance()
    array.valuesList.addAll(csvValues)

    return array
}

private fun createStringArray(value: String): Types.StringArray {
    val csvValues = value.split(CSV_DELIMITER)

    val array = Types.StringArray.getDefaultInstance()
    array.valuesList.addAll(csvValues)

    return array
}

private fun createFloatArray(value: String): Types.FloatArray {
    val csvValues = value.split(CSV_DELIMITER).map { it.toFloat() }

    val array = Types.FloatArray.getDefaultInstance()
    array.valuesList.addAll(csvValues)

    return array
}
