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

import org.eclipse.kuksa.vssprocessor.parser.VssDataKey
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.COMMENT
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.DATATYPE
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.DESCRIPTION
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.MAX
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.MIN
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.TYPE
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.UNIT
import org.eclipse.kuksa.vssprocessor.parser.VssDataKey.UUID
import kotlin.reflect.KClass

internal class VssNodePropertiesBuilder(
    uuid: String,
    type: String,
) {
    private val nodePropertyMap: MutableMap<VssDataKey, VssNodeProperty> = mutableMapOf()

    init {
        val uuidNodeProperty = VssNodeProperty(UUID, uuid, String::class)
        nodePropertyMap[UUID] = uuidNodeProperty

        val typeNodeProperty = VssNodeProperty(TYPE, type, String::class)
        nodePropertyMap[TYPE] = typeNodeProperty
    }

    fun withDescription(description: String): VssNodePropertiesBuilder {
        if (description.isEmpty()) return this

        val nodeProperty = VssNodeProperty(DESCRIPTION, description, String::class)
        nodePropertyMap[DESCRIPTION] = nodeProperty

        return this
    }

    fun withComment(comment: String): VssNodePropertiesBuilder {
        if (comment.isEmpty()) return this

        val nodeProperty = VssNodeProperty(COMMENT, comment, String::class)
        nodePropertyMap[COMMENT] = nodeProperty

        return this
    }

    fun withDataType(dataType: String): VssNodePropertiesBuilder {
        if (dataType.isEmpty()) return this

        val valueDataType = findKClass(dataType)

        val signalProperty = VssSignalProperty(DATATYPE, dataType, valueDataType)
        nodePropertyMap[DATATYPE] = signalProperty

        return this
    }

    fun withUnit(unit: String): VssNodePropertiesBuilder {
        if (unit.isEmpty()) return this

        val signalProperty = VssSignalProperty(UNIT, unit, String::class)
        nodePropertyMap[UNIT] = signalProperty

        return this
    }

    fun withMin(min: String, clazz: KClass<*>): VssNodePropertiesBuilder {
        if (min.isEmpty()) return this

        val signalProperty = VssSignalProperty(MIN, min, clazz)
        nodePropertyMap[MIN] = signalProperty

        return this
    }

    fun withMin(min: String, dataType: String): VssNodePropertiesBuilder {
        if (min.isEmpty() || dataType.isEmpty()) return this

        val valueDataType = findKClass(dataType)

        return withMin(min, valueDataType)
    }

    fun withMax(max: String, clazz: KClass<*>): VssNodePropertiesBuilder {
        if (max.isEmpty()) return this

        val maxSignalProperty = VssSignalProperty(MAX, max, clazz)
        nodePropertyMap[MAX] = maxSignalProperty

        return this
    }

    fun withMax(max: String, dataType: String): VssNodePropertiesBuilder {
        if (max.isEmpty() || dataType.isEmpty()) return this

        val valueDataType = findKClass(dataType)

        return withMax(max, valueDataType)
    }

    private fun findKClass(dataType: String): KClass<*> {
        val vssDataType = VssDataType.find(dataType)
        return vssDataType.valueDataType
    }

    fun build(): Set<VssNodeProperty> {
        return nodePropertyMap.values.toSet()
    }
}
