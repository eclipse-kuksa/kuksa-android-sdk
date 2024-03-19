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

@file:OptIn(ExperimentalUnsignedTypes::class)

package org.eclipse.kuksa.vssprocessor.spec

import kotlin.reflect.KClass

/**
 * The [dataType] is the compatible Kotlin representation of the VSS type. The [stringRepresentation] is the string
 * literal which was used by the VSS standard. The [defaultValue] returns a valid default values as a string literal.
 * Use the [valueDataType] if Java compatibility needs to be ensured because some [dataType]s are using Kotlin inline
 * types which are not supported by Java e.g. [UInt].
 */
enum class VssDataType(
    val dataType: KClass<*>,
    val stringRepresentation: String,
    val defaultValue: String,
    val valueDataType: KClass<*> = dataType,
) {
    UNKNOWN(Any::class, "Any", "null"),
    STRING(String::class, "string", "\"\""),
    BOOL(Boolean::class, "boolean", "false"),
    INT8(Int::class, "int8", "0"),
    INT16(Int::class, "int16", "0"),
    INT32(Int::class, "int32", "0"),
    INT64(Long::class, "int64", "0L"),
    UINT8(Int::class, "uint8", "0", Int::class),
    UINT16(UInt::class, "uint16", "0", Int::class),
    UINT32(UInt::class, "uint32", "0", Int::class),
    UINT64(ULong::class, "uint64", "0L", Long::class),
    FLOAT(Float::class, "float", "0f"),
    DOUBLE(Double::class, "double", "0.0"),
    STRING_ARRAY(Array<String>::class, "string[]", "emptyArray<String>()"),
    BOOL_ARRAY(BooleanArray::class, "boolean[]", "BooleanArray(0)"),
    INT8_ARRAY(IntArray::class, "int8[]", "IntArray(0)"),
    INT16_ARRAY(IntArray::class, "int16[]", "IntArray(0)"),
    INT32_ARRAY(IntArray::class, "int32[]", "IntArray(0)"),
    INT64_ARRAY(LongArray::class, "int64[]", "LongArray(0)"),
    UINT8_ARRAY(UIntArray::class, "uint8[]", "IntArray(0)", IntArray::class),
    UINT16_ARRAY(UIntArray::class, "uint16[]", "IntArray(0)", IntArray::class),
    UINT32_ARRAY(UIntArray::class, "uint32[]", "IntArray(0)", IntArray::class),
    UINT64_ARRAY(ULongArray::class, "uint64[]", "LongArray(0)", LongArray::class),
    FLOAT_ARRAY(FloatArray::class, "float[]", "FloatArray(0)"),
    DOUBLE_ARRAY(DoubleArray::class, "double[]", "DoubleArray(0)"),
    ;

    companion object {
        /**
         * Find the correct [VssDataType] by the given [stringRepresentation]. Returns [UNKNOWN] for undefined
         * [stringRepresentation]s.
         */
        fun find(stringRepresentation: String): VssDataType {
            return entries.find { it.stringRepresentation == stringRepresentation } ?: UNKNOWN
        }
    }
}
