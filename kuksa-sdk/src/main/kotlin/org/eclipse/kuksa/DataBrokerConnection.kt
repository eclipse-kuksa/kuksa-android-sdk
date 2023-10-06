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
import org.eclipse.kuksa.extension.TAG
import org.eclipse.kuksa.extension.copy
import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.KuksaValV1
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse
import org.eclipse.kuksa.proto.v1.KuksaValV1.SubscribeResponse
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.proto.v1.VALGrpc
import org.eclipse.kuksa.vsscore.model.VssProperty
import org.eclipse.kuksa.vsscore.model.VssSpecification
import org.eclipse.kuksa.vsscore.model.heritage

/**
 * The DataBrokerConnection holds an active connection to the DataBroker. The Connection can be use to interact with the
 * DataBroker.
 *
 * @param managedChannel the channel on which communication takes place.
 */
class DataBrokerConnection internal constructor(
    private val managedChannel: ManagedChannel,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    @Suppress("unused")
    val subscriptions: Set<Property>
        get() = subscribedProperties.copy()

    private val subscribedProperties = mutableSetOf<Property>()

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
                Log.v(TAG, "onNext() called with: value = $value")

                for (entryUpdate in value.updatesList) {
                    val entry = entryUpdate.entry
                    propertyObserver.onPropertyChanged(entry.path, entry)
                }
            }

            override fun onError(t: Throwable?) {
                Log.e(TAG, "onError() called with: t = $t, cause: ${t?.cause}")
            }

            override fun onCompleted() {
                Log.d(TAG, "onCompleted() called")
            }
        }

        try {
            asyncStub.subscribe(request, callback)

            subscribedProperties.addAll(properties)
        } catch (e: StatusRuntimeException) {
            throw DataBrokerException(e.message, e)
        }
    }

    /**
     * Subscribes to the specified [VssSpecification] with the provided [VssSpecificationObserver]. Only a [VssProperty]
     * can be subscribed because they have an actual value. When provided with any parent [VssSpecification] then this
     * [subscribe] method will find all [VssProperty] children and subscribes them instead. Once subscribed the
     * application will be notified about any changes to every subscribed [VssProperty].
     *
     * @param specification the [VssSpecification] to subscribe to
     * @param fields the [Types.Field] to subscribe to. The default value is a list with a
     * single [Types.Field.FIELD_VALUE] entry.
     * @param observer the observer to notify in case of changes
     *
     * @throws DataBrokerException in case the connection to the DataBroker is no longer active
     */
    @Suppress("exceptions:TooGenericExceptionCaught") // Handling is bundled together
    @JvmOverloads
    fun <T : VssSpecification> subscribe(
        specification: T,
        fields: List<Types.Field> = listOf(Types.Field.FIELD_VALUE),
        observer: VssSpecificationObserver<T>,
    ) {
        val vssPathToVssProperty = specification.heritage
            .ifEmpty { setOf(specification) }
            .filterIsInstance<VssProperty<*>>() // Only final leafs with a value can be observed
            .groupBy { it.vssPath }
            .mapValues { it.value.first() } // Always one result because the vssPath is unique
        val leafProperties = vssPathToVssProperty.values
            .map { Property(it.vssPath, fields) }
            .toList()

        try {
            Log.d(TAG, "Subscribing to the following properties: $leafProperties")

            // TODO: Remove as soon as the server supports subscribing to vssPaths which are not VssProperties
            // Reduces the load on the observer for big VssSpecifications. We wait for the initial update
            // of all VssProperties before notifying the observer about the first batch
            val initialSubscriptionUpdates = leafProperties.associate { it.vssPath to false }.toMutableMap()

            // This is currently needed because we get multiple subscribe responses for every heir. Otherwise we
            // would override the last heir value with every new response.
            var updatedVssSpecification = specification
            subscribe(leafProperties) { vssPath, updatedValue ->
                Log.v(TAG, "Update from subscribed property: $vssPath - $updatedValue")

                updatedVssSpecification = updatedVssSpecification.copy(vssPath, updatedValue.value)

                initialSubscriptionUpdates[vssPath] = true
                val isInitialSubscriptionComplete = initialSubscriptionUpdates.values.all { it }
                if (isInitialSubscriptionComplete) {
                    Log.d(TAG, "Initial update for subscribed property complete: $vssPath - $updatedValue")
                    observer.onSpecificationChanged(updatedVssSpecification)
                }
            }
        } catch (e: Exception) {
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
    suspend fun fetch(property: Property): GetResponse {
        Log.d(TAG, "fetchProperty() called with: property: $property")
        return withContext(dispatcher) {
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
     * Retrieves the [VssSpecification] and returns it. The retrieved [VssSpecification]
     * is of the same type as the inputted one. All underlying heirs are changed to reflect the data broker state.
     *
     * @param specification to retrieve
     * @param fields to retrieve. The default value is a list with a single [Types.Field.FIELD_VALUE] entry.
     *
     * @throws DataBrokerException in case the connection to the DataBroker is no longer active
     */
    @Suppress("exceptions:TooGenericExceptionCaught") // Handling is bundled together
    @JvmOverloads
    suspend fun <T : VssSpecification> fetch(
        specification: T,
        fields: List<Types.Field> = listOf(Types.Field.FIELD_VALUE),
    ): T {
        return withContext(dispatcher) {
            try {
                val property = Property(specification.vssPath, fields)
                val response = fetch(property)
                val entries = response.entriesList

                if (entries.isEmpty()) {
                    Log.w(TAG, "No entries found for fetched specification!")
                    return@withContext specification
                }

                // Update every heir specification
                // TODO: Can be optimized to not replace the whole heritage line for every child entry one by one
                var updatedSpecification: T = specification
                val heritage = updatedSpecification.heritage
                entries.forEach { entry ->
                    updatedSpecification = updatedSpecification.copy(entry.path, entry.value, heritage)
                }

                return@withContext updatedSpecification
            } catch (e: Exception) {
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
    suspend fun update(
        property: Property,
        updatedDatapoint: Datapoint,
    ): SetResponse {
        Log.d(TAG, "updateProperty() called with: updatedProperty = $property")
        return withContext(dispatcher) {
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
}
