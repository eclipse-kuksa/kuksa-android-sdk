/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.kuksa.mocking

import org.eclipse.kuksa.connectivity.databroker.listener.VssPathListener
import org.eclipse.kuksa.proto.v1.KuksaValV1

class FriendlyVssPathListener : VssPathListener {
    val updates = mutableListOf<List<KuksaValV1.EntryUpdate>>()
    val errors = mutableListOf<Throwable>()
    override fun onEntryChanged(entryUpdates: List<KuksaValV1.EntryUpdate>) {
        updates.add(entryUpdates)
    }

    override fun onError(throwable: Throwable) {
        errors.add(throwable)
    }

    fun reset() {
        updates.clear()
        errors.clear()
    }
}
