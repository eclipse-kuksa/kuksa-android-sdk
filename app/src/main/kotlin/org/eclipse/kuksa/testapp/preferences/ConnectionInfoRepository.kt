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

package org.eclipse.kuksa.testapp.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfoSerializer

class ConnectionInfoRepository(context: Context) {

    private val Context.dataStore: DataStore<ConnectionInfo> by dataStore(PREFERENCES_NAME, ConnectionInfoSerializer)
    private val dataStore = context.dataStore

    val connectionInfoFlow: Flow<ConnectionInfo> = dataStore.data

    suspend fun updateConnectionInfo(connectionInfo: ConnectionInfo) {
        dataStore.updateData { connectionInfo }
    }

    private companion object {
        private const val PREFERENCES_NAME = "connection_info"
    }
}
