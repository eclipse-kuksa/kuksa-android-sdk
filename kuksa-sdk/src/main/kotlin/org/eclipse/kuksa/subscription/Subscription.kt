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
import io.grpc.Context
import io.grpc.stub.StreamObserver
import org.eclipse.kuksa.PropertyObserver
import org.eclipse.kuksa.pattern.listener.MultiListener
import org.eclipse.kuksa.proto.v1.KuksaValV1.SubscribeResponse
import org.eclipse.kuksa.proto.v1.Types.Field

/**
 * Denotes a Subscription to the DataBroker. Will be notified about changes w.r.t. the specified [vssPath] and [field].
 * To get informed about these changes it is required to register an [PropertyObserver] using [observers].
 * [cancellableContext] is used to cancel the subscription. When the Subscription is canceled the communication channel
 * to the DataBroker is closed, no more updates will be received from that point on.
 *
 * Subscriptions are managed by the [SubscriptionManager] it will automatically create new Subscription if none is
 * existing, resp. add the observer to the corresponding Subscription. If all Observers are unregistered the
 * Subscription will be automatically canceled.
 */
internal class Subscription(
    val vssPath: String,
    val field: Field,
    private val cancellableContext: Context.CancellableContext,
) {
    val observers: MultiListener<PropertyObserver> = MultiListener(
        onRegistered = { observer ->
            // initial update on registration
            if (lastThrowable != null) {
                lastThrowable?.let { observer.onError(it) }
            } else {
                val lastSubscribeResponse = lastSubscribeResponse ?: return@MultiListener

                for (entryUpdate in lastSubscribeResponse.updatesList) {
                    val entry = entryUpdate.entry
                    observer.onPropertyChanged(vssPath, field, entry)
                }
            }
        },
    )

    private var lastSubscribeResponse: SubscribeResponse? = null
    private var lastThrowable: Throwable? = null

    /**
     * Cancels the Subscription to the DataBroker, after canceling no more updates will be received from that point on.
     */
    fun cancel() {
        cancellableContext.close()
    }

    override fun toString(): String {
        val identifier = toIdentifier(vssPath, field)
        return "Subscription($identifier)"
    }

    companion object {
        fun toIdentifier(vssPath: String, field: Field): String {
            return "$vssPath#${field.name}"
        }
    }

    inner class SubscriptionStreamObserver : StreamObserver<SubscribeResponse> {
        override fun onNext(value: SubscribeResponse) {
            for (entryUpdate in value.updatesList) {
                val entry = entryUpdate.entry

                observers.forEach { observer ->
                    observer.onPropertyChanged(vssPath, field, entry)
                }
            }
            lastSubscribeResponse = value
        }

        override fun onError(throwable: Throwable?) {
            observers.forEach { observer ->
                throwable?.let { observer.onError(it) }
            }

            lastThrowable = throwable
        }

        override fun onCompleted() {
            Log.d("TAG", "onCompleted() called")
        }
    }
}
