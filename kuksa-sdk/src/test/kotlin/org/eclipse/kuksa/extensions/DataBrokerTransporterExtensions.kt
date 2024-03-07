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

package org.eclipse.kuksa.extensions

import io.kotest.assertions.fail
import org.eclipse.kuksa.connectivity.databroker.DataBrokerTransporter
import org.eclipse.kuksa.proto.v1.Types
import kotlin.random.Random

internal suspend fun DataBrokerTransporter.updateRandomFloatValue(
    vssPath: String,
    maxValue: Int = 300,
): Float {
    val random = Random(System.nanoTime())
    val randomValue = random.nextInt(maxValue)
    val randomFloat = randomValue.toFloat()
    val updatedDatapoint = Types.Datapoint.newBuilder().setFloat(randomFloat).build()

    try {
        update(vssPath, updatedDatapoint, setOf(Types.Field.FIELD_VALUE))
    } catch (e: Exception) {
        fail("Updating $vssPath to $randomFloat failed: $e")
    }

    return randomFloat
}

internal suspend fun DataBrokerTransporter.updateRandomUint32Value(
    vssPath: String,
    maxValue: Int = 300,
): Int {
    val random = Random(System.nanoTime())
    val randomValue = random.nextInt(maxValue)
    val updatedDatapoint = Types.Datapoint.newBuilder().setUint32(randomValue).build()

    try {
        update(vssPath, updatedDatapoint, setOf(Types.Field.FIELD_VALUE))
    } catch (e: Exception) {
        fail("Updating $vssPath to $randomValue failed: $e")
    }

    return randomValue
}

internal suspend fun DataBrokerTransporter.toggleBoolean(vssPath: String): Boolean {
    val fields = setOf(Types.Field.FIELD_VALUE)

    var newBoolean: Boolean? = null
    try {
        val response = fetch(vssPath, fields)
        val currentBool = response.entriesList[0].value.bool

        newBoolean = !currentBool
        val newDatapoint = Types.Datapoint.newBuilder().setBool(newBoolean).build()

        update(vssPath, newDatapoint, fields)
    } catch (e: Exception) {
        fail("Updating $vssPath to $newBoolean failed: $e")
    }

    return newBoolean == true
}
