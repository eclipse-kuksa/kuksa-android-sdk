/*
 * Copyright (c) 2023 - 2025 Contributors to the Eclipse Foundation
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

package com.example.sample

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.grpc.ChannelCredentials
import io.grpc.Grpc
import io.grpc.ManagedChannelBuilder
import io.grpc.TlsChannelCredentials
import kotlinx.coroutines.launch
import org.eclipse.kuksa.connectivity.authentication.JsonWebToken
import org.eclipse.kuksa.connectivity.databroker.DataBrokerException
import org.eclipse.kuksa.connectivity.databroker.DisconnectListener
import org.eclipse.kuksa.connectivity.databroker.v1.DataBrokerConnection
import org.eclipse.kuksa.connectivity.databroker.v1.DataBrokerConnector
import org.eclipse.kuksa.connectivity.databroker.v1.listener.VssNodeListener
import org.eclipse.kuksa.connectivity.databroker.v1.listener.VssPathListener
import org.eclipse.kuksa.connectivity.databroker.v1.request.FetchRequest
import org.eclipse.kuksa.connectivity.databroker.v1.request.SubscribeRequest
import org.eclipse.kuksa.connectivity.databroker.v1.request.UpdateRequest
import org.eclipse.kuksa.connectivity.databroker.v1.request.VssNodeFetchRequest
import org.eclipse.kuksa.connectivity.databroker.v1.request.VssNodeSubscribeRequest
import org.eclipse.kuksa.connectivity.databroker.v1.request.VssNodeUpdateRequest
import org.eclipse.kuksa.proto.v1.KuksaValV1
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.velocitas.vss.VssVehicle
import java.io.IOException

@Suppress("UNUSED_VARIABLE", "SwallowedException")
class KotlinActivity : AppCompatActivity() {

    private var disconnectListener = DisconnectListener {
        // connection closed manually or unexpectedly
    }

    private var dataBrokerConnection: DataBrokerConnection? = null

    fun connectInsecure(host: String, port: Int) {
        lifecycleScope.launch {
            val managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build()

            // or jsonWebToken = null when authentication is disabled
            val jsonWebToken = JsonWebToken("someValidToken")
            val connector = DataBrokerConnector(managedChannel, jsonWebToken)
            try {
                dataBrokerConnection = connector.connect()
                dataBrokerConnection?.disconnectListeners?.register(disconnectListener)
                // Connection to DataBroker successfully established
            } catch (e: DataBrokerException) {
                // Connection to DataBroker failed
            }
        }
    }

    fun connectSecure(host: String, port: Int, overrideAuthority: String) {
        val tlsCredentials: ChannelCredentials
        try {
            val rootCertFile = assets.open("CA.pem")
            tlsCredentials = TlsChannelCredentials.newBuilder()
                .trustManager(rootCertFile)
                .build()
        } catch (_: IOException) {
            // Handle error
            return
        }

        val channelBuilder = Grpc
            .newChannelBuilderForAddress(host, port, tlsCredentials)

        val hasOverrideAuthority = overrideAuthority.isNotEmpty()
        if (hasOverrideAuthority) {
            channelBuilder.overrideAuthority(overrideAuthority)
        }

        lifecycleScope.launch {
            val managedChannel = channelBuilder.build()

            // or jsonWebToken = null when authentication is disabled
            val jsonWebToken = JsonWebToken("someValidToken")
            val connector = DataBrokerConnector(managedChannel, jsonWebToken)
            try {
                dataBrokerConnection = connector.connect()
                    .apply {
                        disconnectListeners.register(disconnectListener)
                    }
                // Connection to DataBroker successfully established
            } catch (e: DataBrokerException) {
                // Connection to DataBroker failed
            }
        }
    }

    fun disconnect() {
        dataBrokerConnection?.disconnectListeners?.unregister(disconnectListener)
        dataBrokerConnection?.disconnect()
        dataBrokerConnection = null
    }

    fun fetchProperty() {
        val request = FetchRequest("Vehicle.Speed", Types.Field.FIELD_VALUE)
        lifecycleScope.launch {
            try {
                val response = dataBrokerConnection?.fetch(request) ?: return@launch
                // handle response
            } catch (e: DataBrokerException) {
                // handle error
            }
        }
    }

    private val newSpeed = 50f

    fun updateProperty() {
        val datapoint = Datapoint.newBuilder()
            .setFloat(newSpeed)
            .build()
        val request = UpdateRequest("Vehicle.Speed", datapoint, Types.Field.FIELD_VALUE)
        lifecycleScope.launch {
            try {
                val response = dataBrokerConnection?.update(request) ?: return@launch
                // handle response
            } catch (e: DataBrokerException) {
                // handle error
            }
        }
    }

    fun subscribeProperty() {
        val request = SubscribeRequest("Vehicle.Speed", Types.Field.FIELD_VALUE)
        val vssPathListener = object : VssPathListener {
            override fun onEntryChanged(entryUpdates: List<KuksaValV1.EntryUpdate>) {
                entryUpdates.forEach { entryUpdate ->
                    val updatedValue = entryUpdate.entry

                    // handle value change
                    when (updatedValue.path) {
                        "Vehicle.Speed" -> {
                            val speed = updatedValue.value.float
                        }
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                // handle error
            }
        }

        dataBrokerConnection?.subscribe(request, vssPathListener)
    }

    // region: VSS generated models
    fun fetchNode() {
        lifecycleScope.launch {
            try {
                val vssSpeed = VssVehicle.VssSpeed()
                val request = VssNodeFetchRequest(vssSpeed, Types.Field.FIELD_VALUE)
                val updatedSpeed = dataBrokerConnection?.fetch(request)
                val speed = updatedSpeed?.value
            } catch (e: DataBrokerException) {
                // handle error
            }
        }
    }

    fun updateNode() {
        lifecycleScope.launch {
            val vssSpeed = VssVehicle.VssSpeed(value = 100f)
            val request = VssNodeUpdateRequest(vssSpeed, Types.Field.FIELD_VALUE)
            dataBrokerConnection?.update(request)
        }
    }

    fun subscribeNode() {
        val vssSpeed = VssVehicle.VssSpeed(value = 100f)
        val request = VssNodeSubscribeRequest(vssSpeed, Types.Field.FIELD_VALUE)
        dataBrokerConnection?.subscribe(
            request,
            object : VssNodeListener<VssVehicle.VssSpeed> {
                override fun onNodeChanged(vssNode: VssVehicle.VssSpeed) {
                    val speed = vssSpeed.value
                }

                override fun onError(throwable: Throwable) {
                    // handle error
                }
            },
        )
    }
    // endregion
}
