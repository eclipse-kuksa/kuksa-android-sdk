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
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.launch
import org.eclipse.kuksa.connectivity.databroker.DataBrokerConnection
import org.eclipse.kuksa.connectivity.databroker.DataBrokerConnector
import org.eclipse.kuksa.connectivity.databroker.DataBrokerException
import org.eclipse.kuksa.connectivity.databroker.listener.DisconnectListener
import org.eclipse.kuksa.connectivity.databroker.listener.PropertyListener
import org.eclipse.kuksa.connectivity.databroker.listener.VssNodeListener
import org.eclipse.kuksa.connectivity.databroker.request.FetchRequest
import org.eclipse.kuksa.connectivity.databroker.request.SubscribeRequest
import org.eclipse.kuksa.connectivity.databroker.request.UpdateRequest
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeFetchRequest
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeSubscribeRequest
import org.eclipse.kuksa.coroutine.CoroutineCallback
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse
import org.eclipse.kuksa.testapp.databroker.connection.DataBrokerConnectorFactory
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo
import org.eclipse.kuksa.vsscore.model.VssNode

@Suppress("complexity:TooManyFunctions")
class KotlinDataBrokerEngine(
    private val lifecycleScope: LifecycleCoroutineScope,
) : DataBrokerEngine {
    override var dataBrokerConnection: DataBrokerConnection? = null

    private val connectorFactory = DataBrokerConnectorFactory()
    private val disconnectListeners = mutableSetOf<DisconnectListener>()

    // Too many to usefully handle: Checked Exceptions: IOE, RuntimeExceptions: UOE, ISE, IAE, ...
    @Suppress("TooGenericExceptionCaught")
    override fun connect(
        context: Context,
        connectionInfo: ConnectionInfo,
        callback: CoroutineCallback<DataBrokerConnection>,
    ) {
        val connector: DataBrokerConnector = try {
            connectorFactory.create(context, connectionInfo)
        } catch (e: Exception) {
            callback.onError(e)
            return
        }

        lifecycleScope.launch {
            try {
                dataBrokerConnection = connector.connect()
                    .also { connection ->
                        disconnectListeners.forEach { listener -> connection.disconnectListeners.register(listener) }
                    }

                callback.onSuccess(dataBrokerConnection)
            } catch (e: DataBrokerException) {
                callback.onError(e)
            }
        }
    }

    override fun fetch(request: FetchRequest, callback: CoroutineCallback<GetResponse>) {
        lifecycleScope.launch {
            try {
                val response = dataBrokerConnection?.fetch(request) ?: return@launch
                callback.onSuccess(response)
            } catch (e: DataBrokerException) {
                callback.onError(e)
            }
        }
    }

    override fun <T : VssNode> fetch(request: VssNodeFetchRequest<T>, callback: CoroutineCallback<T>) {
        lifecycleScope.launch {
            try {
                val response = dataBrokerConnection?.fetch(request) ?: return@launch
                callback.onSuccess(response)
            } catch (e: DataBrokerException) {
                callback.onError(e)
            }
        }
    }

    override fun update(request: UpdateRequest, callback: CoroutineCallback<SetResponse>) {
        lifecycleScope.launch {
            try {
                val response = dataBrokerConnection?.update(request) ?: return@launch
                callback.onSuccess(response)
            } catch (e: DataBrokerException) {
                callback.onError(e)
            }
        }
    }

    override fun subscribe(request: SubscribeRequest, propertyListener: PropertyListener) {
        dataBrokerConnection?.subscribe(request, propertyListener)
    }

    override fun unsubscribe(request: SubscribeRequest, propertyListener: PropertyListener) {
        dataBrokerConnection?.unsubscribe(request, propertyListener)
    }

    override fun <T : VssNode> subscribe(
        request: VssNodeSubscribeRequest<T>,
        vssNodeListener: VssNodeListener<T>,
    ) {
        lifecycleScope.launch {
            dataBrokerConnection?.subscribe(request, listener = vssNodeListener)
        }
    }

    override fun <T : VssNode> unsubscribe(
        request: VssNodeSubscribeRequest<T>,
        vssNodeListener: VssNodeListener<T>,
    ) {
        dataBrokerConnection?.unsubscribe(request, listener = vssNodeListener)
    }

    override fun disconnect() {
        dataBrokerConnection?.disconnect()
        dataBrokerConnection = null
    }

    override fun registerDisconnectListener(listener: DisconnectListener) {
        disconnectListeners.add(listener)
        dataBrokerConnection?.disconnectListeners?.register(listener)
    }

    override fun unregisterDisconnectListener(listener: DisconnectListener) {
        disconnectListeners.remove(listener)
        dataBrokerConnection?.disconnectListeners?.unregister(listener)
    }
}
