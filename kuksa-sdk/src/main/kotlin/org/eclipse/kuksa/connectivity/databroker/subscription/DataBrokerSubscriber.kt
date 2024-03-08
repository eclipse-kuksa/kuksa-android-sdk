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

package org.eclipse.kuksa.connectivity.databroker.subscription

import android.util.Log
import org.eclipse.kuksa.connectivity.databroker.DataBrokerException
import org.eclipse.kuksa.connectivity.databroker.DataBrokerTransporter
import org.eclipse.kuksa.connectivity.databroker.listener.VssNodeListener
import org.eclipse.kuksa.connectivity.databroker.listener.VssPathListener
import org.eclipse.kuksa.extension.TAG
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Field
import org.eclipse.kuksa.vsscore.model.VssNode
import org.eclipse.kuksa.vsscore.model.VssSignal

/**
 * Creates [DataBrokerSubscription]s to the DataBroker to get notified about changes on the underlying vssPaths and
 * fields.If no [DataBrokerSubscription] for a given vssPath and field does exist the DataBrokerSubscriber will create
 * a new one. If it was already requested before, the same [DataBrokerSubscription] will be re-used. When the last
 * [VssPathListener] of a [DataBrokerSubscription] unsubscribes the [DataBrokerSubscription] will be automatically
 * canceled and removed from the active [DataBrokerSubscription]s.
 */
internal class DataBrokerSubscriber(private val dataBrokerTransporter: DataBrokerTransporter) {
    private val subscriptions =
        mutableMapOf<String, DataBrokerSubscription>() // String(Subscription#identifier) -> Subscription

    /**
     * Checks if the SDK is already subscribed to the corresponding [vssPath] and [field], if the SDK is already
     * subscribed it will simply add the 3rd-party [listener] to the current subscription. If not, a new
     * Subscription is made and the [listener] is added to it.
     */
    fun subscribe(vssPath: String, field: Field, listener: VssPathListener) {
        val identifier = createIdentifier(vssPath, field)
        var subscription = subscriptions[identifier]
        if (subscription == null) {
            subscription = dataBrokerTransporter.subscribe(vssPath, field)
            subscriptions[identifier] = subscription
            Log.v(TAG, "Created $subscription")
        }

        subscription.listeners.register(listener)
    }

    /**
     * Removes the specified [listener] for the specified [vssPath] and [field] from an already existing
     * Subscription to the DataBroker. If the given Subscription has no more Listeners after unsubscribing it will be
     * canceled and removed. Gracefully ignores invalid input, e.g. when a [vssPath] and [field] of a non-subscribed
     * [vssPath] is provided.
     */
    fun unsubscribe(vssPath: String, field: Field, listener: VssPathListener) {
        val identifier = createIdentifier(vssPath, field)
        val subscription = subscriptions[identifier] ?: return
        subscription.listeners.unregister(listener)

        if (subscription.listeners.isEmpty()) {
            Log.v(TAG, "Removing $subscription: no more listeners")
            subscription.cancel()
            subscriptions.remove(identifier)
        }
    }

    /**
     * Subscribes to the specified [VssNode] with the provided [VssNodeListener]. Only a [VssSignal]
     * can be subscribed because they have an actual value. When provided with any parent [VssNode] then this
     * [subscribe] method will find all [VssSignal] children and subscribes them instead. Once subscribed the
     * application will be notified about any changes to every subscribed [VssSignal]. The [field] can be used to
     * subscribe to different information of the [node]. The default for the [field] parameter is a single
     * [Types.Field.FIELD_VALUE] entry.
     *
     * @throws DataBrokerException in case the connection to the DataBroker is no longer active
     */
    fun <T : VssNode> subscribe(
        node: T,
        field: Field = Field.FIELD_VALUE,
        listener: VssNodeListener<T>,
    ) {
        val vssPath = node.vssPath

        val vssNodePathListener = VssNodePathListener(node, listener)
        subscribe(vssPath, field, vssNodePathListener)
    }

    /**
     * Removes the specified [listener] for the specified [node] and [field] from an already existing
     * Subscription to the DataBroker. If the given Subscription has no more Listeners after unsubscribing it will be
     * canceled and removed. Gracefully ignores invalid input, e.g. when a [node] and [field] of a
     * non-subscribed [VssNode] is provided.
     */
    fun <T : VssNode> unsubscribe(
        node: T,
        field: Field = Field.FIELD_VALUE,
        listener: VssNodeListener<T>,
    ) {
        val vssPath = node.vssPath

        val vssNodePathListener = VssNodePathListener(node, listener)
        unsubscribe(vssPath, field, vssNodePathListener)
    }

    private companion object {
        private fun createIdentifier(vssPath: String, field: Field): String {
            return "$vssPath#${field.name}"
        }
    }
}
