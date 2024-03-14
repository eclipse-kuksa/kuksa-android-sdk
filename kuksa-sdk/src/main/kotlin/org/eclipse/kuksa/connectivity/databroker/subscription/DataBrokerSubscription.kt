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

import io.grpc.Context
import org.eclipse.kuksa.connectivity.databroker.listener.VssPathListener
import org.eclipse.kuksa.pattern.listener.MultiListener
import org.eclipse.kuksa.proto.v1.KuksaValV1.SubscribeResponse
import org.eclipse.kuksa.proto.v1.Types.Field

/**
 * Denotes a Subscription to the DataBroker. Will be notified about changes w.r.t. the specified [vssPath] and [field].
 * To get informed about these changes it is required to register an [VssPathListener] using [listeners].
 * [cancellableContext] is used to cancel the subscription. When the Subscription is canceled the communication channel
 * to the DataBroker is closed, no more updates will be received from that point on.
 *
 * Subscriptions are managed by the [DataBrokerSubscriber] it will automatically create new Subscription if none is
 * existing, resp. add the observer to the corresponding Subscription. If all Listeners are unregistered the
 * Subscription will be automatically canceled.
 */
internal class DataBrokerSubscription(
    val vssPath: String,
    val field: Field,
    private val cancellableContext: Context.CancellableContext,
) {
    val listeners: MultiListener<VssPathListener> = MultiListener(
        onRegistered = { observer ->
            // initial update on registration
            if (lastThrowable != null) {
                lastThrowable?.let { observer.onError(it) }
            } else {
                val lastSubscribeResponse = lastSubscribeResponse ?: return@MultiListener

                observer.onEntryChanged(lastSubscribeResponse.updatesList)
            }
        },
    )

    internal var lastSubscribeResponse: SubscribeResponse? = null
    internal var lastThrowable: Throwable? = null

    /**
     * Cancels the Subscription to the DataBroker, after canceling no more updates will be received from that point on.
     */
    fun cancel() {
        cancellableContext.close()
    }

    override fun toString(): String {
        return "Subscription(vssPath='$vssPath', field=$field)"
    }
}
