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
import org.eclipse.kuksa.PropertyObserver
import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import java.io.IOException

@Suppress("UNUSED_VARIABLE", "SwallowedException", "UNUSED_ANONYMOUS_PARAMETER")
class KotlinActivity : AppCompatActivity() {

    private var dataBrokerConnection: DataBrokerConnection? = null

    fun connectInsecure(host: String, port: Int) {
        lifecycleScope.launch {
            val managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build()

            val connector = DataBrokerConnector(managedChannel)
            try {
                dataBrokerConnection = connector.connect()
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
                // Connection to DataBroker successfully established
            } catch (e: DataBrokerException) {
                // Connection to DataBroker failed
            }
        }
    }

    fun fetchProperty(property: Property) {
        lifecycleScope.launch {
            try {
                val response = dataBrokerConnection?.fetchProperty(property) ?: return@launch
                // handle response
            } catch (e: DataBrokerException) {
                // handle errors
            }
        }
    }

    fun updateProperty(property: Property, datapoint: Datapoint) {
        lifecycleScope.launch {
            try {
                val response = dataBrokerConnection?.updateProperty(property, datapoint) ?: return@launch
                // handle response
            } catch (e: DataBrokerException) {
                // handle errors
            }
        }
    }

    fun subscribeProperty(property: Property) {
        val propertyObserver = PropertyObserver { vssPath, updatedValue ->
            // handle property change
        }
        val properties = listOf(property)
        dataBrokerConnection?.subscribe(properties, propertyObserver)
    }

    fun disconnect() {
        dataBrokerConnection?.disconnect()
    }
}
