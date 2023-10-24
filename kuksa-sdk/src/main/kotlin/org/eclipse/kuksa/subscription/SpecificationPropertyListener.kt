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
import org.eclipse.kuksa.PropertyListener
import org.eclipse.kuksa.VssSpecificationListener
import org.eclipse.kuksa.extension.TAG
import org.eclipse.kuksa.extension.copy
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.vsscore.model.VssSpecification

internal class SpecificationPropertyListener<T : VssSpecification>(
    specification: T,
    vssPaths: Collection<String>,
    private val observer: VssSpecificationListener<T>,
) : PropertyListener {
    // TODO: Remove as soon as the server supports subscribing to vssPaths which are not VssProperties
    // Reduces the load on the observer for big VssSpecifications. We wait for the initial update
    // of all VssProperties before notifying the observer about the first batch
    private val initialSubscriptionUpdates = vssPaths.associateWith { false }.toMutableMap()

    // This is currently needed because we get multiple subscribe responses for every heir. Otherwise we
    // would override the last heir value with every new response.
    private var updatedVssSpecification: T = specification

    override fun onPropertyChanged(vssPath: String, field: Types.Field, updatedValue: Types.DataEntry) {
        Log.v(TAG, "Update from subscribed property: $vssPath - $field - $updatedValue")
        updatedVssSpecification = updatedVssSpecification.copy(vssPath, updatedValue.value)

        initialSubscriptionUpdates[vssPath] = true
        val isInitialSubscriptionComplete = initialSubscriptionUpdates.values.all { it }
        if (isInitialSubscriptionComplete) {
            Log.d(TAG, "Initial update for subscribed property complete: $vssPath - $updatedValue")
            observer.onSpecificationChanged(updatedVssSpecification)
        }
    }

    override fun onError(throwable: Throwable) {
        observer.onError(throwable)
    }

    // two SpecificationObserverWrapper instances are equal if they have the same observer set!
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpecificationPropertyListener<*>

        if (observer != other.observer) return false

        return true
    }

    override fun hashCode(): Int {
        return observer.hashCode()
    }
}
