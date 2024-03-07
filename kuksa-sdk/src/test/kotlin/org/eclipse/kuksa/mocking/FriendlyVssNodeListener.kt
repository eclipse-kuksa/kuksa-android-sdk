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

import org.eclipse.kuksa.connectivity.databroker.listener.VssNodeListener
import org.eclipse.kuksa.vsscore.model.VssNode

class FriendlyVssNodeListener<T : VssNode> : VssNodeListener<T> {
    val updatedVssNodes = mutableListOf<T>()
    val errors = mutableListOf<Throwable>()

    override fun onNodeChanged(vssNode: T) {
        updatedVssNodes.add(vssNode)
    }

    override fun onError(throwable: Throwable) {
        errors.add(throwable)
    }

    fun reset() {
        updatedVssNodes.clear()
        errors.clear()
    }
}
