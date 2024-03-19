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

package org.eclipse.kuksa.vssprocessor.spec

import kotlin.reflect.KClass

open class VssNodeProperty(
    val vssPath: String,
    val nodePropertyName: String,
    val nodePropertyValue: String,
    val dataType: KClass<*>,
) {
    open val isCommon: Boolean = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VssNodeProperty

        return nodePropertyName == other.nodePropertyName
    }

    override fun hashCode(): Int {
        return nodePropertyName.hashCode()
    }
}

class VssSignalProperty(
    vssPath: String,
    nodePropertyName: String,
    nodePropertyValue: String,
    dataType: KClass<*>,
) : VssNodeProperty(vssPath, nodePropertyName, nodePropertyValue, dataType) {
    override val isCommon: Boolean = false
}
