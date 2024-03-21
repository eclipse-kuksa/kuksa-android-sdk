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

package org.eclipse.kuksa.testapp.databroker.vssnodes.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.eclipse.kuksa.vss.VssVehicle
import org.eclipse.kuksa.vsscore.model.VssNode
import org.eclipse.kuksa.vsscore.model.VssSignal
import org.eclipse.kuksa.vsscore.model.heritage

class VssNodesViewModel : ViewModel() {
    var onGetNode: (node: VssNode) -> Unit = { }
    var onUpdateSignal: (signal: VssSignal<*>) -> Unit = { }
    var onSubscribeNode: (node: VssNode) -> Unit = { }
    var onUnsubscribeNode: (node: VssNode) -> Unit = { }

    var subscribedNodes = mutableStateListOf<VssNode>()

    val isSubscribed by derivedStateOf {
        subscribedNodes.contains(node)
    }

    private val vssVehicle = VssVehicle()
    val nodes = listOf(vssVehicle) + vssVehicle.heritage

    var node: VssNode by mutableStateOf(vssVehicle)
        private set

    fun updateNode(node: VssNode) {
        this.node = node
    }
}
