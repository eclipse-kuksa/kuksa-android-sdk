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
import org.eclipse.kuksa.connectivity.databroker.DataBrokerConnection
import org.eclipse.kuksa.connectivity.databroker.listener.DisconnectListener
import org.eclipse.kuksa.connectivity.databroker.listener.VssNodeListener
import org.eclipse.kuksa.connectivity.databroker.listener.VssPathListener
import org.eclipse.kuksa.connectivity.databroker.request.FetchRequest
import org.eclipse.kuksa.connectivity.databroker.request.SubscribeRequest
import org.eclipse.kuksa.connectivity.databroker.request.UpdateRequest
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeFetchRequest
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeSubscribeRequest
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeUpdateRequest
import org.eclipse.kuksa.connectivity.databroker.response.VssNodeUpdateResponse
import org.eclipse.kuksa.coroutine.CoroutineCallback
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse
import org.eclipse.kuksa.testapp.databroker.connection.model.ConnectionInfo
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
        request: FetchRequest,
        callback: CoroutineCallback<GetResponse>,
    )

    fun <T : VssNode> fetch(request: VssNodeFetchRequest<T>, callback: CoroutineCallback<T>)

    fun update(
        request: UpdateRequest,
        callback: CoroutineCallback<SetResponse>,
    )

    fun <T : VssNode> update(
        request: VssNodeUpdateRequest<T>,
        callback: CoroutineCallback<VssNodeUpdateResponse>,
    )

    fun subscribe(request: SubscribeRequest, listener: VssPathListener)

    fun <T : VssNode> subscribe(
        request: VssNodeSubscribeRequest<T>,
        vssNodeListener: VssNodeListener<T>,
    )

    fun unsubscribe(request: SubscribeRequest, listener: VssPathListener)

    fun <T : VssNode> unsubscribe(
        request: VssNodeSubscribeRequest<T>,
        vssNodeListener: VssNodeListener<T>,
    )

    fun disconnect()

    fun registerDisconnectListener(listener: DisconnectListener)

    fun unregisterDisconnectListener(listener: DisconnectListener)
}
