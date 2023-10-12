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

package org.eclipse.kuksa.testapp.serialization

import android.util.Log
import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.eclipse.kuksa.testapp.extension.TAG
import java.io.InputStream
import java.io.OutputStream

abstract class JsonSerializer<T>(private val serializer: KSerializer<T>) : Serializer<T> {
    override suspend fun readFrom(input: InputStream): T {
        return try {
            withContext(Dispatchers.IO) {
                val string = input.readBytes().decodeToString()
                Json.decodeFromString(
                    serializer,
                    string,
                )
            }
        } catch (e: SerializationException) {
            Log.w(TAG, "Unable to deserialize - falling back to defaultValue", e)
            defaultValue
        }
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        withContext(Dispatchers.IO) {
            val encodedString = Json.encodeToString(serializer, t)
            output.write(encodedString.encodeToByteArray())
        }
    }
}
