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

package org.eclipse.kuksa.testapp.databroker.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.eclipse.kuksa.extension.createDatapoint
import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.proto.v1.Types.Datapoint.ValueCase
import org.eclipse.kuksa.proto.v1.Types.Field

class VSSPropertiesViewModel : ViewModel() {
    var onGetProperty: (property: Property) -> Unit = { }
    var onSetProperty: (property: Property, datapoint: Datapoint) -> Unit = { _: Property, _: Datapoint -> }
    var onSubscribeProperty: (property: Property) -> Unit = { }
    var onUnsubscribeProperty: (property: Property) -> Unit = { }

    var subscribedProperties = mutableStateListOf<Property>()

    val isSubscribed by derivedStateOf {
        subscribedProperties.contains(property)
    }

    var vssProperties: VSSProperties by mutableStateOf(VSSProperties())
        private set

    val valueTypes: List<ValueCase> = ValueCase.values().toList()
    val fieldTypes: List<Field> = listOf(Field.FIELD_VALUE, Field.FIELD_ACTUATOR_TARGET)

    val datapoint: Datapoint
        get() = vssProperties.valueType.createDatapoint(vssProperties.value)

    // Meta data are always part of the properties
    val property: Property
        get() = Property(vssProperties.vssPath, listOf(vssProperties.fieldType, Field.FIELD_METADATA))

    fun updateVssProperties(vssProperties: VSSProperties = VSSProperties()) {
        this.vssProperties = vssProperties
    }
}

@Immutable
data class VSSProperties(
    val vssPath: String = "Vehicle.Speed",
    val valueType: ValueCase = ValueCase.VALUE_NOT_SET,
    val value: String = "130",
    val fieldType: Field = Field.FIELD_VALUE,
)
