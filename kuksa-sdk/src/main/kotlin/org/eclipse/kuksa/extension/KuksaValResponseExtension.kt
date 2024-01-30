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

import org.eclipse.kuksa.proto.v1.KuksaValV1
import org.eclipse.kuksa.proto.v1.Types

/**
 * Convenience property which returns any [Types.Metadata] if available.
 */
val KuksaValV1.GetResponse.entriesMetadata: List<Types.Metadata>
    get() {
        if (entriesList.isEmpty()) return emptyList()

        return entriesList.map { it.metadata }
    }

/**
 * Convenience property which returns the first value ([Types.Datapoint]) from the [KuksaValV1.GetResponse].
 */
val KuksaValV1.GetResponse.firstValue: Types.Datapoint?
    get() {
        if (entriesList.isEmpty()) return null

        val entry = entriesList.first()
        if (!entry.hasValue()) {
            return null
        }

        return entry.value
    }
