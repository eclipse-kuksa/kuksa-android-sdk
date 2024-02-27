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
import io.grpc.ManagedChannel
import kotlinx.coroutines.launch
import org.eclipse.kuksa.CoroutineCallback
import org.eclipse.kuksa.DataBrokerConnection
import org.eclipse.kuksa.DataBrokerConnector
import org.eclipse.kuksa.DataBrokerException
import org.eclipse.kuksa.DisconnectListener
import org.eclipse.kuksa.PropertyListener
import org.eclipse.kuksa.TimeoutConfig
import org.eclipse.kuksa.VssSpecificationListener
import org.eclipse.kuksa.authentication.JsonWebToken
import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo
import org.eclipse.kuksa.testapp.extension.createInsecureManagedChannel
import org.eclipse.kuksa.testapp.extension.createSecureManagedChannel
import org.eclipse.kuksa.testapp.extension.loadJsonWebToken
import org.eclipse.kuksa.vsscore.model.VssSpecification

@Suppress("complexity:TooManyFunctions")
class KotlinDataBrokerEngine(
    private val lifecycleScope: LifecycleCoroutineScope,
) : DataBrokerEngine {
    override var dataBrokerConnection: DataBrokerConnection? = null

    private val disconnectListeners = mutableSetOf<DisconnectListener>()

    // Too many to usefully handle: Checked Exceptions: IOE, RuntimeExceptions: UOE, ISE, IAE, ...
    @Suppress("TooGenericExceptionCaught")
    override fun connect(
        context: Context,
        connectionInfo: ConnectionInfo,
        callback: CoroutineCallback<DataBrokerConnection>,
    ) {
        try {
            if (connectionInfo.isTlsEnabled) {
                connectSecure(context, connectionInfo, callback)
            } else {
                connectInsecure(context, connectionInfo, callback)
            }
        } catch (e: Exception) {
            callback.onError(e)
        }
    }

    private fun connectInsecure(
        context: Context,
        connectionInfo: ConnectionInfo,
        callback: CoroutineCallback<DataBrokerConnection>,
    ) {
        val insecureManagedChannel = connectionInfo.createInsecureManagedChannel()
        val jsonWebToken: JsonWebToken? = connectionInfo.loadJsonWebToken(context)

        connect(insecureManagedChannel, jsonWebToken, callback)
    }

    private fun connectSecure(
        context: Context,
        connectionInfo: ConnectionInfo,
        callback: CoroutineCallback<DataBrokerConnection>,
    ) {
        val secureManagedChannel = connectionInfo.createSecureManagedChannel(context)
        val jsonWebToken: JsonWebToken? = connectionInfo.loadJsonWebToken(context)

        connect(secureManagedChannel, jsonWebToken, callback)
    }

    private fun connect(
        managedChannel: ManagedChannel,
        jsonWebToken: JsonWebToken?,
        callback: CoroutineCallback<DataBrokerConnection>,
    ) {
        val connector = DataBrokerConnector(managedChannel, jsonWebToken).apply {
            timeoutConfig = TimeoutConfig(TIMEOUT_CONNECTION_SEC)
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

    override fun fetch(property: Property, callback: CoroutineCallback<GetResponse>) {
        lifecycleScope.launch {
            try {
                val response = dataBrokerConnection?.fetch(property) ?: return@launch
                callback.onSuccess(response)
            } catch (e: DataBrokerException) {
                callback.onError(e)
            }
        }
    }

    override fun <T : VssSpecification> fetch(specification: T, callback: CoroutineCallback<T>) {
        lifecycleScope.launch {
            try {
                val response = dataBrokerConnection?.fetch(specification) ?: return@launch
                callback.onSuccess(response)
            } catch (e: DataBrokerException) {
                callback.onError(e)
            }
        }
    }

    override fun update(property: Property, datapoint: Datapoint, callback: CoroutineCallback<SetResponse>) {
        lifecycleScope.launch {
            try {
                val response = dataBrokerConnection?.update(property, datapoint) ?: return@launch
                callback.onSuccess(response)
            } catch (e: DataBrokerException) {
                callback.onError(e)
            }
        }
    }

    override fun subscribe(property: Property, propertyListener: PropertyListener) {
        dataBrokerConnection?.subscribe(property, propertyListener)
    }

    override fun unsubscribe(property: Property, propertyListener: PropertyListener) {
        dataBrokerConnection?.unsubscribe(property, propertyListener)
    }

    override fun <T : VssSpecification> subscribe(
        specification: T,
        specificationListener: VssSpecificationListener<T>,
    ) {
        lifecycleScope.launch {
            dataBrokerConnection?.subscribe(specification, listener = specificationListener)
        }
    }

    override fun <T : VssSpecification> unsubscribe(
        specification: T,
        specificationListener: VssSpecificationListener<T>,
    ) {
        dataBrokerConnection?.unsubscribe(specification, listener = specificationListener)
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

    companion object {
        const val TIMEOUT_CONNECTION_SEC = 5L
    }
}
