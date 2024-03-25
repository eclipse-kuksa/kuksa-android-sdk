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
const val KEY_CHILDREN = "children"

enum class VssDataKey {
    UUID,
    TYPE,
    DESCRIPTION,
    COMMENT,
    DATATYPE,
    UNIT,
    MIN,
    MAX,
    ;

    val key = name.lowercase()

    companion object {
        fun findByKey(key: String): VssDataKey? {
            return entries.find { it.key == key }
        }
    }
}
