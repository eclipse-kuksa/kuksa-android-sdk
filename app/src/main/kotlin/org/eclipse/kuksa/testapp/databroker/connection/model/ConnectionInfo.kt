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

package org.eclipse.kuksa.testapp.databroker.connection.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import org.eclipse.kuksa.testapp.serialization.JsonSerializer

@Serializable
@Immutable
data class ConnectionInfo(
    val host: String = "localhost",
    val port: Int = 55556,
    val isTlsEnabled: Boolean = false,
    val certificate: Certificate = Certificate.DEFAULT,
    val isAuthenticationEnabled: Boolean = false,
    val jwtUriPath: String? = null,
)

object ConnectionInfoSerializer : JsonSerializer<ConnectionInfo>(ConnectionInfo.serializer()) {
    override val defaultValue: ConnectionInfo
        get() = ConnectionInfo()
}
