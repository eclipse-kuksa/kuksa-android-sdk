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

package org.eclipse.kuksa.testapp.databroker.vsspaths.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.eclipse.kuksa.extension.createDatapoint
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.proto.v1.Types.Datapoint.ValueCase
import org.eclipse.kuksa.proto.v1.Types.Field
import java.util.TreeSet

class VSSPathsViewModel : ViewModel() {
    var onGetProperty: (property: DataBrokerProperty) -> Unit = { }
    var onSetProperty: (property: DataBrokerProperty, datapoint: Datapoint) -> Unit = {
            _: DataBrokerProperty,
            _: Datapoint,
        ->
    }
    var onSubscribeProperty: (property: DataBrokerProperty) -> Unit = { }
    var onUnsubscribeProperty: (property: DataBrokerProperty) -> Unit = { }

    var subscribedProperties = mutableStateListOf<DataBrokerProperty>()

    val isSubscribed by derivedStateOf {
        subscribedProperties.contains(dataBrokerProperty)
    }

    var dataBrokerProperty: DataBrokerProperty by mutableStateOf(DataBrokerProperty())
        private set

    val valueTypes: List<ValueCase> = ValueCase.entries
    val fieldTypes: List<Field> = listOf(
        Field.FIELD_VALUE,
        Field.FIELD_ACTUATOR_TARGET,
        Field.FIELD_METADATA,
    )

    var suggestions: Set<String> by mutableStateOf(setOf())
        private set

    val datapoint: Datapoint
        get() = dataBrokerProperty.valueType.createDatapoint(dataBrokerProperty.value)

    fun updateDataBrokerProperty(property: DataBrokerProperty = DataBrokerProperty()) {
        dataBrokerProperty = property
    }

    fun updateSuggestions(vssPaths: Collection<String>) {
        suggestions = generateVssPathHierarchy(vssPaths)
    }

    private fun generateVssPathHierarchy(paths: Collection<String>): TreeSet<String> {
        val pathSet = TreeSet<String>()

        paths.forEach {
            pathSet.add(it)

            var value = it
            while (value.indexOf(".") > -1) {
                value = value.substringBeforeLast(".")
                pathSet.add(value)
            }
        }

        return pathSet
    }
}

@Immutable
data class DataBrokerProperty(
    val vssPath: String = "Vehicle",
    val valueType: ValueCase = ValueCase.VALUE_NOT_SET,
    val value: String = "",
    val fieldTypes: Collection<Field> = setOf(Field.FIELD_VALUE),
)
