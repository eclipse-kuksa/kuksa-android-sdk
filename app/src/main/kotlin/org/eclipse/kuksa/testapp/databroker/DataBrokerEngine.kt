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

package org.eclipse.kuksa.testapp.databroker

import org.eclipse.kuksa.CoroutineCallback
import org.eclipse.kuksa.DataBrokerConnection
import org.eclipse.kuksa.PropertyObserver
import org.eclipse.kuksa.VssSpecificationObserver
import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.testapp.model.ConnectionInfo
import org.eclipse.kuksa.vsscore.model.VssSpecification

interface DataBrokerEngine {
    var dataBrokerConnection: DataBrokerConnection?

    fun connect(connectionInfo: ConnectionInfo, callback: CoroutineCallback<DataBrokerConnection>)
    fun fetchProperty(property: Property, callback: CoroutineCallback<GetResponse>)
    fun <T : VssSpecification> fetchSpecification(specification: T, observer: VssSpecificationObserver<T>)
    fun updateProperty(
        property: Property,
        datapoint: Datapoint,
        callback: CoroutineCallback<SetResponse>,
    )
    fun subscribe(property: Property, propertyObserver: PropertyObserver)
    fun <T : VssSpecification> subscribe(
        specification: T,
        fields: List<Types.Field> = listOf(Types.Field.FIELD_VALUE, Types.Field.FIELD_METADATA),
        propertyObserver: VssSpecificationObserver<T>,
    )

    fun disconnect()
}
