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

package org.eclipse.kuksa.connectivity.databroker.listener

import org.eclipse.kuksa.pattern.listener.Listener
import org.eclipse.kuksa.proto.v1.KuksaValV1
import org.eclipse.kuksa.vsscore.model.VssNode

/**
 * The Listener is used to notify about changes to subscribed VSS paths. When registering the
 * listener to e.g. Vehicle.ADAS.ABS this listener will also be notified about changes of the children
 * Vehicle.ADAS.ABS.IsEnabled or Vehicle.ADAS.ABS.IsEngaged.
 */
interface VssPathListener : Listener {
    /**
     * Will be triggered with a list of [entryUpdates] of the corresponding field.
     */
    fun onEntryChanged(entryUpdates: List<KuksaValV1.EntryUpdate>)

    /**
     * Will be triggered when an error happens during subscription and forwards the [throwable].
     */
    fun onError(throwable: Throwable)
}

/**
 * The Listener is used to notify about subscribed [VssNode]. If a [VssNode] has children
 * then [onNodeChanged] will be called on every value change for every children.
 */
interface VssNodeListener<T : VssNode> : Listener {
    /**
     * Will be triggered with the [vssNode] when the underlying vssPath changed it's value or to inform about
     * the initial state.
     */
    fun onNodeChanged(vssNode: T)

    /**
     * Will be triggered when an error happens during subscription and forwards the [throwable].
     */
    fun onError(throwable: Throwable)
}
