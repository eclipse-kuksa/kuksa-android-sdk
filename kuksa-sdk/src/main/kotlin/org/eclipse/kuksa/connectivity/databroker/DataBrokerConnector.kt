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

package org.eclipse.kuksa.connectivity.databroker

import android.util.Log
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.eclipse.kuksa.connectivity.authentication.JsonWebToken
import org.eclipse.kuksa.extension.TAG
import org.eclipse.kuksa.model.TimeoutConfig

/**
 * The DataBrokerConnector is used to establish a successful connection to the DataBroker. The communication takes
 * place inside the [managedChannel]. Use the [defaultDispatcher] for the coroutine scope.
 */
class DataBrokerConnector @JvmOverloads constructor(
    private val managedChannel: ManagedChannel,
    private val jsonWebToken: JsonWebToken? = null,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    /**
     * Configuration to be used during connection.
     */
    var timeoutConfig = TimeoutConfig()

    /**
     * Connects to the specified DataBroker.
     *
     * @throws DataBrokerException when connect() is called again while it is trying to connect (fail fast) resp.
     * when no connection to the DataBroker can be established (e.g. after the in #timeoutConfig defined time).
     */
    suspend fun connect(): DataBrokerConnection {
        val connectivityState = managedChannel.getState(false)
        if (connectivityState != ConnectivityState.IDLE) {
            throw DataBrokerException("Connector is already trying to establish a connection")
        }

        Log.d(TAG, "connect() called")

        return withContext(defaultDispatcher) {
            val startTime = System.currentTimeMillis()
            val timeoutMillis = timeoutConfig.timeUnit.toMillis(timeoutConfig.timeout)
            var durationMillis = 0L

            @Suppress("MagicNumber") // self explanatory number
            val delayMillis = 1000L
            var state = managedChannel.getState(true) // is there no other way to connect?
            while (state != ConnectivityState.READY && durationMillis < timeoutMillis - delayMillis) {
                durationMillis = System.currentTimeMillis() - startTime
                state = managedChannel.getState(false)

                delay(delayMillis)
            }

            if (state == ConnectivityState.READY) {
                return@withContext DataBrokerConnection(managedChannel, defaultDispatcher)
                    .apply {
                        jsonWebToken = this@DataBrokerConnector.jsonWebToken
                    }
            } else {
                managedChannel.shutdownNow()
                throw DataBrokerException("timeout")
            }
        }
    }
}
