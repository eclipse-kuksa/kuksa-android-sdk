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

package com.example.sample

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.grpc.ChannelCredentials
import io.grpc.Grpc
import io.grpc.ManagedChannelBuilder
import io.grpc.TlsChannelCredentials
import kotlinx.coroutines.launch
import org.eclipse.kuksa.DataBrokerConnection
import org.eclipse.kuksa.DataBrokerConnector
import org.eclipse.kuksa.DataBrokerException
import org.eclipse.kuksa.DisconnectListener
import org.eclipse.kuksa.PropertyListener
import org.eclipse.kuksa.VssSpecificationListener
import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.KuksaValV1
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.vss.VssVehicle
import org.eclipse.kuksa.vsscore.annotation.VssDefinition
import java.io.IOException

@Suppress("UNUSED_VARIABLE", "SwallowedException")
@VssDefinition
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

            val connector = DataBrokerConnector(managedChannel)
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
            val connector = DataBrokerConnector(managedChannel)
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

    fun fetchProperty(property: Property) {
        lifecycleScope.launch {
            try {
                val response = dataBrokerConnection?.fetch(property) ?: return@launch
                // handle response
            } catch (e: DataBrokerException) {
                // handle error
            }
        }
    }

    fun updateProperty(property: Property, datapoint: Datapoint) {
        lifecycleScope.launch {
            try {
                val response = dataBrokerConnection?.update(property, datapoint) ?: return@launch
                // handle response
            } catch (e: DataBrokerException) {
                // handle error
            }
        }
    }

    fun subscribeProperty(property: Property) {
        val propertyListener = object : PropertyListener {
            override fun onPropertyChanged(entryUpdates: List<KuksaValV1.EntryUpdate>) {
                entryUpdates.forEach { entryUpdate ->
                    val updatedValue = entryUpdate.entry

                    // handle property change
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

        dataBrokerConnection?.subscribe(property, propertyListener)
    }

    // region: Specifications
    fun fetchSpecification() {
        lifecycleScope.launch {
            try {
                val vssSpeed = VssVehicle.VssSpeed()
                val updatedSpeed = dataBrokerConnection?.fetch(vssSpeed, listOf(Types.Field.FIELD_VALUE))
                val speed = updatedSpeed?.value
            } catch (e: DataBrokerException) {
                // handle error
            }
        }
    }

    fun updateSpecification() {
        lifecycleScope.launch {
            val vssSpeed = VssVehicle.VssSpeed(value = 100f)
            dataBrokerConnection?.update(vssSpeed, listOf(Types.Field.FIELD_VALUE))
        }
    }

    fun subscribeSpecification() {
        val vssSpeed = VssVehicle.VssSpeed(value = 100f)
        dataBrokerConnection?.subscribe(
            vssSpeed,
            listOf(Types.Field.FIELD_VALUE),
            listener = object : VssSpecificationListener<VssVehicle.VssSpeed> {
                override fun onSpecificationChanged(vssSpecification: VssVehicle.VssSpeed) {
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
