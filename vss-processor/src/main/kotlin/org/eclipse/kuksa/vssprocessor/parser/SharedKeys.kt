/*
 * Copyright (c) 2023 - 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.kuksa.vssprocessor.parser

const val ROOT_KEY_VEHICLE = "Vehicle"

const val KEY_DATA_DESCRIPTION = "description"
const val KEY_DATA_TYPE = "type"
const val KEY_DATA_UUID = "uuid"
const val KEY_DATA_COMMENT = "comment"
const val KEY_DATA_DATATYPE = "datatype"
const val KEY_DATA_UNIT = "unit"
const val KEY_DATA_MIN = "min"
const val KEY_DATA_MAX = "max"
const val KEY_DATA_CHILDREN = "children"

val VSS_DATA_KEYS = listOf(
    KEY_DATA_DESCRIPTION,
    KEY_DATA_TYPE,
    KEY_DATA_UUID,
    KEY_DATA_COMMENT,
    KEY_DATA_UNIT,
    KEY_DATA_DATATYPE,
    KEY_DATA_MIN,
    KEY_DATA_MAX,
    KEY_DATA_CHILDREN,
)
