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
import org.eclipse.kuksa.proto.v1.Types.Datapoint.ValueCase

/**
 * Returns the converted VSS data types -> Protobuf data types.
 */
val Types.DataType.dataPointValueCase: ValueCase
    get() {
        return when (this) {
            Types.DataType.DATA_TYPE_UNSPECIFIED,
            Types.DataType.UNRECOGNIZED,
            Types.DataType.DATA_TYPE_TIMESTAMP,
            Types.DataType.DATA_TYPE_TIMESTAMP_ARRAY,
            -> ValueCase.VALUE_NOT_SET

            Types.DataType.DATA_TYPE_BOOLEAN -> ValueCase.BOOL
            Types.DataType.DATA_TYPE_INT8,
            Types.DataType.DATA_TYPE_INT16,
            Types.DataType.DATA_TYPE_INT32,
            -> ValueCase.INT32

            Types.DataType.DATA_TYPE_INT64 -> ValueCase.INT64

            Types.DataType.DATA_TYPE_UINT8,
            Types.DataType.DATA_TYPE_UINT16,
            Types.DataType.DATA_TYPE_UINT32,
            -> ValueCase.UINT32

            Types.DataType.DATA_TYPE_UINT64 -> ValueCase.UINT64

            Types.DataType.DATA_TYPE_INT8_ARRAY,
            Types.DataType.DATA_TYPE_INT16_ARRAY,
            Types.DataType.DATA_TYPE_INT32_ARRAY,
            -> ValueCase.INT32_ARRAY

            Types.DataType.DATA_TYPE_INT64_ARRAY -> ValueCase.INT64_ARRAY

            Types.DataType.DATA_TYPE_UINT8_ARRAY,
            Types.DataType.DATA_TYPE_UINT16_ARRAY,
            Types.DataType.DATA_TYPE_UINT32_ARRAY,
            -> ValueCase.UINT32_ARRAY

            Types.DataType.DATA_TYPE_UINT64_ARRAY -> ValueCase.UINT64_ARRAY

            Types.DataType.DATA_TYPE_STRING -> ValueCase.STRING
            Types.DataType.DATA_TYPE_FLOAT -> ValueCase.FLOAT
            Types.DataType.DATA_TYPE_DOUBLE -> ValueCase.DOUBLE

            Types.DataType.DATA_TYPE_BOOLEAN_ARRAY -> ValueCase.BOOL_ARRAY
            Types.DataType.DATA_TYPE_FLOAT_ARRAY -> ValueCase.FLOAT_ARRAY
            Types.DataType.DATA_TYPE_DOUBLE_ARRAY -> ValueCase.DOUBLE_ARRAY
            Types.DataType.DATA_TYPE_STRING_ARRAY -> ValueCase.STRING_ARRAY
        }
    }
