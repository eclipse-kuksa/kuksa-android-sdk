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
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.kuksa.extension.TAG
import org.eclipse.kuksa.extension.copy
import org.eclipse.kuksa.extension.datapoint
import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.pattern.listener.MultiListener
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.proto.v1.Types.Field
import org.eclipse.kuksa.subscription.DataBrokerSubscriber
import org.eclipse.kuksa.vsscore.model.VssProperty
import org.eclipse.kuksa.vsscore.model.VssSpecification
import org.eclipse.kuksa.vsscore.model.heritage
import org.eclipse.kuksa.vsscore.model.vssProperties

/**
 * The DataBrokerConnection holds an active connection to the DataBroker. The Connection can be use to interact with the
 * DataBroker.
 */
class DataBrokerConnection internal constructor(
    private val managedChannel: ManagedChannel,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val dataBrokerTransporter: DataBrokerTransporter = DataBrokerTransporter(
        managedChannel,
        dispatcher,
    ),
    private val dataBrokerSubscriber: DataBrokerSubscriber = DataBrokerSubscriber(dataBrokerTransporter),
) {
    val disconnectListeners = MultiListener<DisconnectListener>()

    init {
        val state = managedChannel.getState(false)
        managedChannel.notifyWhenStateChanged(state) {
            val newState = managedChannel.getState(false)
            Log.d(TAG, "DataBrokerConnection state changed: $newState")
            if (newState != ConnectivityState.SHUTDOWN) {
                managedChannel.shutdownNow()
            }

            disconnectListeners.forEach { listener ->
                listener.onDisconnect()
            }
        }
    }

    /**
     * Subscribes to the specified [property] and notifies the provided [propertyListener] about updates.
     *
     * Throws a [DataBrokerException] in case the connection to the DataBroker is no longer active
     */
    fun subscribe(
        property: Property,
        propertyListener: PropertyListener,
    ) {
        val vssPath = property.vssPath
        property.fields.forEach { field ->
            dataBrokerSubscriber.subscribe(vssPath, field, propertyListener)
        }
    }

    /**
     * Unsubscribes the [propertyListener] from updates of the specified [property].
     */
    fun unsubscribe(
        property: Property,
        propertyListener: PropertyListener,
    ) {
        val vssPath = property.vssPath
        property.fields.forEach { field ->
            dataBrokerSubscriber.unsubscribe(vssPath, field, propertyListener)
        }
    }

    /**
     * Subscribes to the specified [VssSpecification] with the provided [VssSpecificationListener]. Only a [VssProperty]
     * can be subscribed because they have an actual value. When provided with any parent [VssSpecification] then this
     * [subscribe] method will find all [VssProperty] children and subscribes them instead. Once subscribed the
     * application will be notified about any changes to every subscribed [VssProperty]. The [fields] can be used to
     * subscribe to different information of the [specification]. The default for the [fields] parameter is a list with
     * a single [Types.Field.FIELD_VALUE] entry.
     *
     * @throws DataBrokerException in case the connection to the DataBroker is no longer active
     */
    @JvmOverloads
    fun <T : VssSpecification> subscribe(
        specification: T,
        fields: Collection<Field> = listOf(Field.FIELD_VALUE),
        listener: VssSpecificationListener<T>,
    ) {
        fields.forEach { field ->
            dataBrokerSubscriber.subscribe(specification, field, listener)
        }
    }

    /**
     * Unsubscribes the [listener] from updates of the specified [fields] and [specification].
     */
    fun <T : VssSpecification> unsubscribe(
        specification: T,
        fields: Collection<Field> = listOf(Field.FIELD_VALUE),
        listener: VssSpecificationListener<T>,
    ) {
        fields.forEach { field ->
            dataBrokerSubscriber.unsubscribe(specification, field, listener)
        }
    }

    /**
     * Retrieves the underlying property of the specified vssPath and returns it to the corresponding Callback.
     *
     * @throws DataBrokerException in case the connection to the DataBroker is no longer active
     */
    suspend fun fetch(property: Property): GetResponse {
        Log.d(TAG, "fetchProperty() called with: property: $property")
        return dataBrokerTransporter.fetch(property.vssPath, property.fields)
    }

    /**
     * Retrieves the [VssSpecification] and returns it. The retrieved [VssSpecification]
     * is of the same type as the inputted one. All underlying heirs are changed to reflect the data broker state.
     * The [fields] can be used to subscribe to different information of the [specification]. The default for the
     * [fields] parameter is a list with a single [Types.Field.FIELD_VALUE] entry.
     *
     * @throws DataBrokerException in case the connection to the DataBroker is no longer active
     */
    @Suppress("exceptions:TooGenericExceptionCaught") // Handling is bundled together
    @JvmOverloads
    suspend fun <T : VssSpecification> fetch(
        specification: T,
        fields: Collection<Field> = listOf(Field.FIELD_VALUE),
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
     * @throws DataBrokerException in case the connection to the DataBroker is no longer active
     */
    suspend fun update(
        property: Property,
        datapoint: Datapoint,
    ): SetResponse {
        Log.d(TAG, "updateProperty() called with: updatedProperty = $property")
        return dataBrokerTransporter.update(property.vssPath, property.fields, datapoint)
    }

    /**
     * Only a [VssProperty] can be updated because they have an actual value. When provided with any parent
     * [VssSpecification] then this [update] method will find all [VssProperty] children and updates their corresponding
     * [fields] instead.
     * Compared to [update] with only one [Property] and [Datapoint], here multiple [SetResponse] will be returned
     * because a [VssSpecification] may consists of multiple values which may need to be updated.
     *
     * @throws DataBrokerException in case the connection to the DataBroker is no longer active
     * @throws IllegalArgumentException if the [VssProperty] could not be converted to a [Datapoint].
     */
    @JvmOverloads
    suspend fun update(
        vssSpecification: VssSpecification,
        fields: Collection<Field> = listOf(Field.FIELD_VALUE),
    ): Collection<SetResponse> {
        val responses = mutableListOf<SetResponse>()

        vssSpecification.vssProperties.forEach { vssProperty ->
            val property = Property(vssProperty.vssPath, fields)
            val response = update(property, vssProperty.datapoint)
            responses.add(response)
        }

        return responses
    }

    /**
     * Disconnect from the DataBroker.
     */
    fun disconnect() {
        Log.d(TAG, "disconnect() called")
        managedChannel.shutdownNow()
    }
}
