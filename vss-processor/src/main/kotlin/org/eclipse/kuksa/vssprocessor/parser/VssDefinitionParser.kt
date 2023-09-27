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

package org.eclipse.kuksa.vssprocessor.parser

import org.eclipse.kuksa.vssprocessor.spec.VssSpecificationSpecModel
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

internal interface VssDefinitionParser {
    /**
     * @param definitionFile to parse [VssSpecificationSpecModel] with
     */
    fun parseSpecifications(definitionFile: File): List<VssSpecificationSpecModel>
}

/**
 * @param fields to set via reflection. Pair<PropertyName, anyValue>.
 * @param remapNames which can be used if the propertyName does not match with the input name
 */
internal fun VssSpecificationSpecModel.setFields(
    fields: List<Pair<String, Any?>>,
    remapNames: Map<String, String> = emptyMap(),
) {
    val nameToProperty = this::class.memberProperties.associateBy(KProperty<*>::name)

    val remappedFields = fields.toMutableList()
    remapNames.forEach { (propertyName, newName) ->
        val find = fields.find { it.first == propertyName } ?: return@forEach
        remappedFields.remove(find)
        remappedFields.add(Pair(find.first, newName))
    }

    remappedFields.forEach { (propertyName, propertyValue) ->
        nameToProperty[propertyName]
            .takeIf { it is KMutableProperty<*> }
            ?.let { it as KMutableProperty<*> }
            ?.setter?.call(this, propertyValue)
    }
}
