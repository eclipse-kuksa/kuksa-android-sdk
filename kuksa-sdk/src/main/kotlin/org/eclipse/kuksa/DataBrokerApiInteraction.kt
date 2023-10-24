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

import io.grpc.ConnectivityState
import io.grpc.Context
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.kuksa.proto.v1.KuksaValV1
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Field
import org.eclipse.kuksa.proto.v1.VALGrpc
import org.eclipse.kuksa.subscription.Subscription

/**
 * Encapsulates the Protobuf-specific interactions with the DataBroker using gRPC. The DataBrokerApiInteraction requires
 * a [managedChannel] which is already connected to the corresponding DataBroker.
 *
 * @throws IllegalStateException in case the state of the [managedChannel] is not [ConnectivityState.READY]
 */
internal class DataBrokerApiInteraction(
    private val managedChannel: ManagedChannel,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    init {
        val state = managedChannel.getState(false)
        check(state == ConnectivityState.READY) {
            "ManagedChannel needs to be connected to the target"
        }
    }

    /**
     * Sends a request to the DataBroker to respond with the specified [vssPath] and [fields] values.
     *
     * Throws a [DataBrokerException] in case the connection to the DataBroker is no longer active
     */
    suspend fun fetch(
        vssPath: String,
        fields: List<Field>,
    ): KuksaValV1.GetResponse {
        return withContext(defaultDispatcher) {
            val blockingStub = VALGrpc.newBlockingStub(managedChannel)
            val entryRequest = KuksaValV1.EntryRequest.newBuilder()
                .setPath(vssPath)
                .addAllFields(fields)
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
     * Sends a request to the DataBroker to update the specified [fields] of the [vssPath] and replace it's value with
     * the specified [updatedDatapoint].
     *
     * Throws a [DataBrokerException] in case the connection to the DataBroker is no longer active
     */
    suspend fun update(
        vssPath: String,
        fields: List<Field>,
        updatedDatapoint: Types.Datapoint,
    ): KuksaValV1.SetResponse {
        return withContext(defaultDispatcher) {
            val blockingStub = VALGrpc.newBlockingStub(managedChannel)
            val dataEntry = Types.DataEntry.newBuilder()
                .setPath(vssPath)
                .setValue(updatedDatapoint)
                .build()
            val entryUpdate = KuksaValV1.EntryUpdate.newBuilder()
                .setEntry(dataEntry)
                .addAllFields(fields)
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
     * Sends a request to the DataBroker to subscribe to updates of the specified [vssPath] and [field].
     * Returns a [Subscription] which can be used to register or unregister additional listeners or cancel / closing
     * the subscription.
     *
     * Throws a [DataBrokerException] in case the connection to the DataBroker is no longer active
     */
    fun subscribe(
        vssPath: String,
        field: Field,
    ): Subscription {
        val asyncStub = VALGrpc.newStub(managedChannel)

        val subscribeEntry = KuksaValV1.SubscribeEntry.newBuilder()
            .setPath(vssPath)
            .addFields(field)
            .build()

        val request = KuksaValV1.SubscribeRequest.newBuilder()
            .addEntries(subscribeEntry)
            .build()

        val currentContext = Context.current()
        val cancellableContext = currentContext.withCancellation()

        val subscription = Subscription(vssPath, field, cancellableContext)
        cancellableContext.run {
            try {
                val streamObserver = subscription.SubscriptionStreamObserver()
                asyncStub.subscribe(request, streamObserver)
            } catch (e: StatusRuntimeException) {
                throw DataBrokerException(e.message, e)
            }
        }

        return subscription
    }
}
