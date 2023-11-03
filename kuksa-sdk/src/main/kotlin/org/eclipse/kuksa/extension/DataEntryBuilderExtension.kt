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
import org.eclipse.kuksa.proto.v1.Types.Field

/**
 * Applies the given [datapoint] to the given [Types.DataEntry.Builder]. If the [field] is set to
 * [Field.FIELD_ACTUATOR_TARGET] it will set the datapoint using [Types.DataEntry.Builder.setActuatorTarget],
 * otherwise it it will set the datapoint using [Types.DataEntry.Builder.setValue].
 */
fun Types.DataEntry.Builder.applyDatapoint(
    datapoint: Types.Datapoint,
    field: Field,
): Types.DataEntry.Builder {
    when (field) {
        Field.FIELD_ACTUATOR_TARGET -> {
            this.actuatorTarget = datapoint
        }

        else -> {
            this.value = datapoint
        }
    }

    return this
}
