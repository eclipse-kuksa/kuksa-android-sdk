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

package org.eclipse.kuksa.subscription

import android.util.Log
import org.eclipse.kuksa.DataBrokerException
import org.eclipse.kuksa.DataBrokerTransporter
import org.eclipse.kuksa.PropertyListener
import org.eclipse.kuksa.VssSpecificationListener
import org.eclipse.kuksa.extension.TAG
import org.eclipse.kuksa.extension.createProperties
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Field
import org.eclipse.kuksa.vsscore.model.VssProperty
import org.eclipse.kuksa.vsscore.model.VssSpecification

/**
 * Creates [Subscription]s to the DataBroker to get notified about changes on the underlying vssPaths and fields.
 * If no [Subscription] for a given vssPath and field does exist the DataBrokerSubscriber will create a new one. If it
 * was already requested before, the same [Subscription] will be re-used.
 * When the last [PropertyListener] of a [Subscription] unsubscribes the [Subscription] will be automatically canceled
 * and removed from the active [Subscription]s.
 */
internal class DataBrokerSubscriber(private val dataBrokerTransporter: DataBrokerTransporter) {
    private val subscriptions = mutableMapOf<String, Subscription>() // String(Subscription#identifier) -> Subscription

    /**
     * Checks if the SDK is already subscribed to the corresponding [vssPath] and [field], if the SDK is already
     * subscribed it will simply add the 3rd-party [propertyListener] to the current subscription. If not, a new
     * Subscription is made and the [propertyListener] is added to it.
     */
    fun subscribe(vssPath: String, field: Field, propertyListener: PropertyListener) {
        val identifier = createIdentifier(vssPath, field)
        var subscription = subscriptions[identifier]
        if (subscription == null) {
            subscription = dataBrokerTransporter.subscribe(vssPath, field)
            subscriptions[identifier] = subscription
            Log.v(TAG, "Created $subscription")
        }

        subscription.listeners.register(propertyListener)
    }

    /**
     * Removes the specified [propertyListener] for the specified [vssPath] and [field] from an already existing
     * Subscription to the DataBroker. If the given Subscription has no more Listeners after unsubscribing it will be
     * canceled and removed. Gracefully ignores invalid input, e.g. when a [vssPath] and [field] of a non-subscribed
     * property is provided.
     */
    fun unsubscribe(vssPath: String, field: Field, propertyListener: PropertyListener) {
        val identifier = createIdentifier(vssPath, field)
        val subscription = subscriptions[identifier] ?: return
        subscription.listeners.unregister(propertyListener)

        if (subscription.listeners.isEmpty()) {
            Log.v(TAG, "Removing $subscription: no more listeners")
            subscription.cancel()
            subscriptions.remove(identifier)
        }
    }

    /**
     * Subscribes to the specified [VssSpecification] with the provided [VssSpecificationListener]. Only a [VssProperty]
     * can be subscribed because they have an actual value. When provided with any parent [VssSpecification] then this
     * [subscribe] method will find all [VssProperty] children and subscribes them instead. Once subscribed the
     * application will be notified about any changes to every subscribed [VssProperty]. The [field] can be used to
     * subscribe to different information of the [specification]. The default for the [field] parameter is a single
     * [Types.Field.FIELD_VALUE] entry.
     *
     * @throws DataBrokerException in case the connection to the DataBroker is no longer active
     */
    fun <T : VssSpecification> subscribe(
        specification: T,
        field: Field = Field.FIELD_VALUE,
        observer: VssSpecificationListener<T>,
    ) {
        val leafProperties = specification.createProperties(field)
        val vssPaths = leafProperties.map { it.vssPath }

        val specificationPropertyListener = SpecificationPropertyListener(specification, vssPaths, observer)
        vssPaths.forEach { vssPath ->
            subscribe(vssPath, field, specificationPropertyListener)
        }
    }

    /**
     * Removes the specified [observer] for the specified [specification] and [field] from an already existing
     * Subscription to the DataBroker. If the given Subscription has no more Listeners after unsubscribing it will be
     * canceled and removed. Gracefully ignores invalid input, e.g. when a [specification] and [field] of a
     * non-subscribed property is provided.
     */
    fun <T : VssSpecification> unsubscribe(
        specification: T,
        field: Field = Field.FIELD_VALUE,
        observer: VssSpecificationListener<T>,
    ) {
        val leafProperties = specification.createProperties(field)
        val vssPaths = leafProperties.map { it.vssPath }

        val specificationPropertyListener = SpecificationPropertyListener(specification, vssPaths, observer)
        vssPaths.forEach { vssPath ->
            unsubscribe(vssPath, field, specificationPropertyListener)
        }
    }

    private companion object {
        private fun createIdentifier(vssPath: String, field: Field): String {
            return "$vssPath#${field.name}"
        }
    }
}
