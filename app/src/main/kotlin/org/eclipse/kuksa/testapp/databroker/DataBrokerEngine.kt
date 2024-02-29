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

import android.content.Context
import org.eclipse.kuksa.CoroutineCallback
import org.eclipse.kuksa.DataBrokerConnection
import org.eclipse.kuksa.DisconnectListener
import org.eclipse.kuksa.PropertyListener
import org.eclipse.kuksa.VssNodeListener
import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo
import org.eclipse.kuksa.vsscore.model.VssNode

@Suppress("complexity:TooManyFunctions") // required to test the api
interface DataBrokerEngine {
    var dataBrokerConnection: DataBrokerConnection?

    fun connect(
        context: Context,
        connectionInfo: ConnectionInfo,
        callback: CoroutineCallback<DataBrokerConnection>,
    )

    fun fetch(
        property: Property,
        callback: CoroutineCallback<GetResponse>,
    )

    fun <T : VssNode> fetch(specification: T, callback: CoroutineCallback<T>)

    fun update(
        property: Property,
        datapoint: Datapoint,
        callback: CoroutineCallback<SetResponse>,
    )

    fun subscribe(property: Property, propertyListener: PropertyListener)

    fun unsubscribe(property: Property, propertyListener: PropertyListener)

    fun <T : VssNode> subscribe(
        specification: T,
        specificationListener: VssNodeListener<T>,
    )

    fun <T : VssNode> unsubscribe(
        specification: T,
        specificationListener: VssNodeListener<T>,
    )

    fun disconnect()

    fun registerDisconnectListener(listener: DisconnectListener)

    fun unregisterDisconnectListener(listener: DisconnectListener)
}
