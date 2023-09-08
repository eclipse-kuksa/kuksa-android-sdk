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

package org.eclipse.kuksa

import android.util.Log
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.KuksaValV1
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse
import org.eclipse.kuksa.proto.v1.KuksaValV1.SubscribeResponse
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.VALGrpc
import org.eclipse.kuksa.util.LogTag

/**
 * The DataBrokerConnection holds an active connection to the DataBroker. The Connection can be use to interact with the
 * DataBroker.
 *
 * @param managedChannel the channel on which communication takes place.
 */
class DataBrokerConnection internal constructor(
    private val managedChannel: ManagedChannel,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    /**
     * Subscribes to the specified vssPath with the provided propertyObserver. Once subscribed the application will be
     * notified about any changes to the specified vssPath.
     *
     * @param properties the properties to subscribe to
     * @param propertyObserver the observer to notify in case of changes
     *
     * @throws DataBrokerException in case the connection to the DataBroker is no longer active
     */
    fun subscribe(
        properties: List<Property>,
        propertyObserver: PropertyObserver,
    ) {
        val asyncStub = VALGrpc.newStub(managedChannel)

        val subscribeEntries = properties.map { property ->
            KuksaValV1.SubscribeEntry.newBuilder()
                .addAllFields(property.fields)
                .setPath(property.vssPath)
                .build()
        }
        val request = KuksaValV1.SubscribeRequest.newBuilder()
            .addAllEntries(subscribeEntries)
            .build()

        val callback = object : StreamObserver<SubscribeResponse> {
            override fun onNext(value: SubscribeResponse) {
                Log.d(TAG, "onNext() called with: value = $value")

                for (entryUpdate in value.updatesList) {
                    val entry = entryUpdate.entry
                    propertyObserver.onPropertyChanged(entry.path, entry)
                }
            }

            override fun onError(t: Throwable?) {
                Log.d(TAG, "onError() called with: t = $t")
            }

            override fun onCompleted() {
                Log.d(TAG, "onCompleted() called")
            }
        }

        try {
            asyncStub.subscribe(request, callback)
        } catch (e: StatusRuntimeException) {
            throw DataBrokerException(e.message, e)
        }
    }

    /**
     * Retrieves the underlying property of the specified vssPath and returns it to the corresponding Callback.
     *
     * @param property the property to retrieve
     *
     * @throws DataBrokerException in case the connection to the DataBroker is no longer active
     */
    suspend fun fetchProperty(property: Property): GetResponse {
        Log.d(TAG, "fetchProperty() called with: property: $property")
        return withContext(defaultDispatcher) {
            val blockingStub = VALGrpc.newBlockingStub(managedChannel)
            val entryRequest = KuksaValV1.EntryRequest.newBuilder()
                .setPath(property.vssPath)
                .addAllFields(property.fields)
                .build()
            val request = KuksaValV1.GetRequest.newBuilder()
                .addEntries(entryRequest)
                .build()

            return@withContext try {
                blockingStub.get(request)
            } catch (e: StatusRuntimeException) {
                throw DataBrokerException(e.message, e)
            }
        }
    }

    /**
     * Updates the underlying property of the specified vssPath with the updatedProperty. Notifies the callback
     * about (un)successful operation.
     *
     * @param property the property to update
     * @param updatedDatapoint the updated datapoint of the property
     *
     * @throws DataBrokerException in case the connection to the DataBroker is no longer active
     */
    suspend fun updateProperty(
        property: Property,
        updatedDatapoint: Types.Datapoint,
    ): SetResponse {
        Log.d(TAG, "updateProperty() called with: updatedProperty = $property")
        return withContext(defaultDispatcher) {
            val blockingStub = VALGrpc.newBlockingStub(managedChannel)
            val dataEntry = Types.DataEntry.newBuilder()
                .setPath(property.vssPath)
                .setValue(updatedDatapoint)
                .build()
            val entryUpdate = KuksaValV1.EntryUpdate.newBuilder()
                .setEntry(dataEntry)
                .addAllFields(property.fields)
                .build()
            val request = KuksaValV1.SetRequest.newBuilder()
                .addUpdates(entryUpdate)
                .build()

            return@withContext try {
                blockingStub.set(request)
            } catch (e: StatusRuntimeException) {
                throw DataBrokerException(e.message, e)
            }
        }
    }

    /**
     * Disconnect from the DataBroker.
     */
    fun disconnect() {
        Log.d(TAG, "disconnect() called")
        managedChannel.shutdown()
    }

    private companion object {
        private val TAG = LogTag.of(DataBrokerConnection::class)
    }
}
