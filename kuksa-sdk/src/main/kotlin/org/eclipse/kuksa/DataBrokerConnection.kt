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
import org.eclipse.kuksa.proto.v1.Types.BoolArray
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.proto.v1.VALGrpc
import org.eclipse.kuksa.util.LogTag
import org.eclipse.kuksa.vsscore.model.VssProperty
import org.eclipse.kuksa.vsscore.model.VssSpecification
import org.eclipse.kuksa.vsscore.model.findHeritageLine
import org.eclipse.kuksa.vsscore.model.heritage
import org.eclipse.kuksa.vsscore.model.name
import kotlin.reflect.KParameter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions

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
                Log.v(TAG, "onNext() called with: value = $value")

                for (entryUpdate in value.updatesList) {
                    val entry = entryUpdate.entry
                    propertyObserver.onPropertyChanged(entry.path, entry)
                }
            }

            override fun onError(t: Throwable?) {
                Log.w(TAG, "onError() called with: t = $t")
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
     *
     */
    @Suppress("LABEL_NAME_CLASH")
    fun <T : VssSpecification> subscribe(
        specification: T,
        fields: List<Types.Field> = listOf(Types.Field.FIELD_VALUE),
        propertyObserver: VssPropertyObserver<T>,
    ) {
        val vssPathToSpecification = specification.heritage
            .ifEmpty { setOf(specification) }
            .filterIsInstance<VssProperty<*>>() // Only final leafs with a value can be observed
            .groupBy { it.vssPath }
            .mapValues { it.value.first() }
        val leafProperties = vssPathToSpecification.values
            .map { Property(it.vssPath, fields) }
            .toList()

        Log.d(TAG, "Subscribing to the following properties: $leafProperties")
        subscribe(leafProperties) { vssPath, updatedValue ->
            Log.d(TAG, "Update from subscribed property: $vssPath - $updatedValue")
            val subscribedVssProperty = vssPathToSpecification[vssPath] ?: return@subscribe

            val updatedVssProperty = subscribedVssProperty.copy(updatedValue.value)
            val relevantChildren = specification.findHeritageLine(subscribedVssProperty).toMutableList()

            // Replace the last specification (Property) with the changed one
            if (relevantChildren.isNotEmpty()) {
                relevantChildren.removeLast()
                relevantChildren.add(updatedVssProperty)
            }

            val updatedVssSpecification = specification.deepCopy(relevantChildren)

            propertyObserver.onPropertyChanged(updatedVssSpecification)
        }
    }

    /**
     * Creates a copy of the [VssSpecification] where the whole [VssSpecification.findHeritageLine] is replaced
     * with modified heirs.
     *
     * Example: VssVehicle->VssCabin->VssWindowChildLockEngaged
     * A deep copy is necessary for a nested history tree with at least two generations. The VssWindowChildLockEngaged
     * is replaced inside VssCabin where this again is replaced inside VssVehicle.
     *
     * @param changedHeritageLine the line of heirs
     * @param generation the generation to start copying with starting from the [VssSpecification] to [deepCopy]
     * @return a copy where every heir in the given [changedHeritageLine] is replaced with a another copy.
     */
    fun <T : VssSpecification> T.deepCopy(changedHeritageLine: List<VssSpecification>, generation: Int = 0): T {
        val childSpecification = changedHeritageLine[generation]
        if (generation == changedHeritageLine.size - 1) { // Reached the end, use the changed VssProperty
            return childSpecification.copy()
        }

        val childCopy = childSpecification.deepCopy(changedHeritageLine, generation + 1)
        val childMap = mapOf(childSpecification.name to childCopy)

        return copy(childMap)
    }

    /**
     * Creates a copy of a [VssProperty] where the [VssProperty.value] is changed to the given [Datapoint].
     *
     * @param datapoint the [Datapoint.value_] is converted to the correct datatype depending on the [VssProperty.value]
     * @return a copy of the [VssProperty] with the updated [VssProperty.value]
     */
    @Suppress("IMPLICIT_CAST_TO_ANY")
    fun <T : Any> VssProperty<T>.copy(datapoint: Datapoint): VssProperty<T> {
        with(datapoint) {
            val value = when (value::class) {
                String::class -> string
                Boolean::class -> bool
                Float::class -> float
                Double::class -> double
                Int::class -> int32
                Long::class -> int64
                Unit::class -> uint64
                Array<String>::class -> stringArray.valuesList
                IntArray::class -> int32Array.valuesList.toIntArray()
                BoolArray::class -> boolArray.valuesList.toBooleanArray()

                else -> string
            }

            val valueMap = mapOf("value" to value)
            return this@copy.copy(valueMap) as VssProperty<T>
        }
    }

    /**
     * Uses reflection to create a copy with any constructor parameter which matches the given [paramToValue] map.
     * It is recommend to only use data classes.
     *
     * @param paramToValue <PropertyName, value> to match the constructor parameters
     * @return a copy of the class
     * @throws [NoSuchElementException] if the class has no "copy" method
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> Any.copy(paramToValue: Map<String, Any?> = emptyMap()): T {
        val instanceClass = this::class

        val copyFunction = instanceClass::memberFunctions.get().first { it.name == "copy" }
        val valueArgs = copyFunction.parameters
            .filter { parameter ->
                parameter.kind == KParameter.Kind.VALUE
            }.mapNotNull { parameter ->
                paramToValue[parameter.name]?.let { value -> parameter to value }
            }

        val copy = copyFunction.callBy(
            mapOf(copyFunction.instanceParameter!! to this) + valueArgs,
        ) ?: this

        return copy as T
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
