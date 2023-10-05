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

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo
import org.eclipse.kuksa.testapp.preferences.ConnectionInfoRepository

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {
    enum class ConnectionViewState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
    }

    var onConnect: (connectionInfo: ConnectionInfo) -> Unit = { }
    var onDisconnect: () -> Unit = { }

    var connectionTimeoutMillis: Long by mutableLongStateOf(TIMEOUT_DEFAULT)
        private set

    private val connectionInfoRepository = ConnectionInfoRepository(application)

    var connectionInfoFlow = connectionInfoRepository.connectionInfoFlow

    var connectionViewState: ConnectionViewState by mutableStateOf(ConnectionViewState.DISCONNECTED)
        private set

    val isConnected by derivedStateOf {
        when (connectionViewState) {
            ConnectionViewState.CONNECTED -> true
            else -> false
        }
    }

    val isConnecting by derivedStateOf {
        when (connectionViewState) {
            ConnectionViewState.CONNECTING -> true
            else -> false
        }
    }
    val isDisconnected by derivedStateOf {
        when (connectionViewState) {
            ConnectionViewState.DISCONNECTED -> true
            else -> false
        }
    }

    fun updateTimeout(timeoutMillis: Long) {
        connectionTimeoutMillis = timeoutMillis
    }

    fun updateConnectionState(state: ConnectionViewState) {
        this.connectionViewState = state
    }

    fun updateConnectionInfo(connectionInfo: ConnectionInfo) {
        viewModelScope.launch {
            connectionInfoRepository.updateConnectionInfo(connectionInfo)
        }
    }

    companion object {
        private const val TIMEOUT_DEFAULT = 5_000L
    }
}
