/*
 * Copyright (c) 2023 - 2025 Contributors to the Eclipse Foundation
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

package org.eclipse.kuksa.extensions

import io.kotest.assertions.fail
import org.eclipse.kuksa.connectivity.databroker.v1.DataBrokerConnection
import org.eclipse.kuksa.connectivity.databroker.v1.request.FetchRequest
import org.eclipse.kuksa.connectivity.databroker.v1.request.UpdateRequest
import org.eclipse.kuksa.proto.v1.Types
import kotlin.random.Random

internal suspend fun DataBrokerConnection.updateRandomFloatValue(
    vssPath: String,
    maxValue: Int = 300,
): Float {
    val random = Random(System.nanoTime())
    val randomValue = random.nextInt(maxValue)
    val randomFloat = randomValue.toFloat()
    val updatedDatapoint = Types.Datapoint.newBuilder().setFloat(randomFloat).build()

    try {
        val updateRequest = UpdateRequest(vssPath, updatedDatapoint)
        update(updateRequest)
    } catch (e: Exception) {
        fail("Updating $vssPath to $randomFloat failed: $e")
    }

    return randomFloat
}

internal suspend fun DataBrokerConnection.updateRandomUint32Value(
    vssPath: String,
    maxValue: Int = 300,
): Int {
    val random = Random(System.nanoTime())
    val randomValue = random.nextInt(maxValue)
    val updatedDatapoint = Types.Datapoint.newBuilder().setUint32(randomValue).build()

    try {
        val updateRequest = UpdateRequest(vssPath, updatedDatapoint)
        update(updateRequest)
    } catch (e: Exception) {
        fail("Updating $vssPath to $randomValue failed: $e")
    }

    return randomValue
}

internal suspend fun DataBrokerConnection.toggleBoolean(vssPath: String): Boolean {
    var newBoolean: Boolean? = null
    try {
        val fetchRequest = FetchRequest(vssPath)
        val response = fetch(fetchRequest)
        val currentBool = response.entriesList[0].value.bool

        newBoolean = !currentBool
        val newDatapoint = Types.Datapoint.newBuilder().setBool(newBoolean).build()

        val updateRequest = UpdateRequest(vssPath, newDatapoint)
        update(updateRequest)
    } catch (e: Exception) {
        fail("Updating $vssPath to $newBoolean failed: $e")
    }

    return newBoolean == true
}
