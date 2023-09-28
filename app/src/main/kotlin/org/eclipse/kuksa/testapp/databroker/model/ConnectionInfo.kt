/*
 *
 *  * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *
 *
 */

package org.eclipse.kuksa.testapp.databroker.model

import androidx.compose.runtime.Immutable
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
@Immutable
data class ConnectionInfo(
    val host: String = "localhost",
    val port: Int = 55556,
    val certificate: Certificate = Certificate.DEFAULT,
    val isTlsEnabled: Boolean = false,
)

object ConnectionInfoSerializer : Serializer<ConnectionInfo> {
    override val defaultValue: ConnectionInfo
        get() = ConnectionInfo()

    override suspend fun readFrom(input: InputStream): ConnectionInfo {
        try {
            return withContext(Dispatchers.IO) {
                val deserializer = ConnectionInfo.serializer()
                Json.decodeFromString(
                    deserializer,
                    input.readBytes().decodeToString(),
                )
            }
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to read UserPrefs", e)
        }
    }

    override suspend fun writeTo(t: ConnectionInfo, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(ConnectionInfo.serializer(), t).encodeToByteArray(),
            )
        }
    }
}
