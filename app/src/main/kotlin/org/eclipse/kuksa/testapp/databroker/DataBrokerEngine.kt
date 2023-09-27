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
import org.eclipse.kuksa.proto.v1.KuksaValV1
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.testapp.model.ConnectionInfo
import org.eclipse.kuksa.vsscore.model.model.VssSpecification

interface DataBrokerEngine {
    var dataBrokerConnection: DataBrokerConnection?

    fun connect(connectionInfo: ConnectionInfo, callback: CoroutineCallback<DataBrokerConnection>)
    suspend fun fetchProperty(property: Property): KuksaValV1.GetResponse?
    suspend fun <T : VssSpecification> fetchSpecification(specification: T): VssSpecification?

    suspend fun updateProperty(
        property: Property,
        datapoint: Datapoint,
    ): SetResponse?

    fun subscribe(property: Property, propertyObserver: PropertyObserver)
    fun <T : VssSpecification> subscribe(
        specification: T,
        propertyObserver: VssSpecificationObserver<T>,
    )

    fun disconnect()
}
