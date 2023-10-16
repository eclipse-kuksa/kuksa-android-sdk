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

package org.eclipse.kuksa.extension

import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.vsscore.model.VssProperty
import org.eclipse.kuksa.vsscore.model.VssSpecification
import org.eclipse.kuksa.vsscore.model.latestGeneration

/**
 * Finds all [VssProperty] heirs for the [VssSpecification] and converts them into a collection of [Pair] with a
 * [Property] and [Datapoint].
 */
fun VssSpecification.createPropertyDataPoints(
    fields: List<Types.Field> = listOf(Types.Field.FIELD_VALUE),
): Collection<Pair<Property, Datapoint>> {
    return latestGeneration
        .map { vssProperty ->
            val property = Property(vssProperty.vssPath, fields)
            val datapoint = vssProperty.datapoint
            Pair(property, datapoint)
        }
}

/**
 * Finds all [VssProperty] heirs for the [VssSpecification] and converts them into a collection of [Property].
 */
fun VssSpecification.createProperties(
    fields: List<Types.Field> = listOf(Types.Field.FIELD_VALUE),
): Collection<Property> {
    return latestGeneration
        .map { Property(it.vssPath, fields) }
}
