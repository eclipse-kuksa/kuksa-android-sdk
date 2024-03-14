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

package org.eclipse.kuksa.testapp.databroker.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TopAppBarViewModel : ViewModel() {
    enum class DataBrokerMode {
        VSS_PATH,
        VSS_FILE,
    }

    var isCompatibilityModeEnabled by mutableStateOf(false)
    var isVssFileModeEnabled by mutableStateOf(false)
        private set

    var onCompatibilityModeChanged: ((Boolean) -> Unit)? = null
    var onDataBrokerModeChanged: ((DataBrokerMode) -> Unit)? = null

    var dataBrokerMode by mutableStateOf(DataBrokerMode.VSS_PATH)
        private set

    init {
        snapshotFlow { isCompatibilityModeEnabled }
            .onEach { onCompatibilityModeChanged?.invoke(it) }
            .launchIn(viewModelScope)

        snapshotFlow { isVssFileModeEnabled }
            .onEach {
                val mode = if (isVssFileModeEnabled) DataBrokerMode.VSS_FILE else DataBrokerMode.VSS_PATH
                onDataBrokerModeChanged?.invoke(mode)
            }
            .launchIn(viewModelScope)

        snapshotFlow { dataBrokerMode }
            .onEach {
                isVssFileModeEnabled = it == DataBrokerMode.VSS_FILE
            }
            .launchIn(viewModelScope)
    }

    fun updateDataBrokerMode(dataBrokerMode: DataBrokerMode) {
        this.dataBrokerMode = dataBrokerMode
    }
}
